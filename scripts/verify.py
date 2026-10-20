"""Offline candidate verification using public sources, built output and an installed SDK.
Writes only .local/. Does not build, install, contact a device or require research inputs.
"""
from pathlib import Path
import argparse, hashlib, json, os, re, shutil, subprocess, sys, zipfile
import xml.etree.ElementTree as ET
sys.dont_write_bytecode = True
from dex_inspect import Dex

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / '.local'
BUILD = OUT / 'build/app'
APP = 'dev.cheeseowl.gmbg'
TARGET = 'com.android.quicksearchbox'
SHA = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
TEXT = lambda p: p.read_text(encoding='utf-8-sig')
checks = []
def check(name, ok):
    checks.append({'check': name, 'passed': bool(ok)})

def public_files():
    return sorted(p for p in ROOT.rglob('*') if p.is_file() and
                  not any(x in {'.local', '.git'} for x in p.relative_to(ROOT).parts))

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sdk', default=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT'))
    parser.add_argument('--java', default=str(Path(os.environ['JAVA_HOME']) / 'bin/java') if os.environ.get('JAVA_HOME') else shutil.which('java'))
    args = parser.parse_args()
    if not args.sdk or not args.java:
        parser.error('Set ANDROID_HOME and JAVA_HOME or pass --sdk and --java')
    java = args.java
    if os.name == 'nt' and not Path(java).is_file() and Path(java + '.exe').is_file():
        java += '.exe'
    tools = Path(args.sdk) / 'build-tools/36.0.0'
    aapt = tools / ('aapt2.exe' if os.name == 'nt' else 'aapt2')
    apk = BUILD / 'outputs/apk/debug/app-debug.apk'
    OUT.mkdir(exist_ok=True)
    private = OUT / 'checks'
    private.mkdir(exist_ok=True)
    outputs = {}
    for name, command in [
        ('badging', [str(aapt), 'dump', 'badging', str(apk)]),
        ('manifest', [str(aapt), 'dump', 'xmltree', str(apk), '--file', 'AndroidManifest.xml']),
        ('signing', [java, '-jar', str(tools / 'lib/apksigner.jar'), 'verify', '--verbose', '--print-certs', str(apk)])
    ]:
        run = subprocess.run(command, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=60)
        outputs[name] = run.stdout + run.stderr
        (private / (name + '.txt')).write_text(outputs[name], encoding='utf-8')
        check(name + ' tool succeeds', run.returncode == 0)
    bad = outputs['badging']
    check('APK applicationId/version', f"name='{APP}'" in bad and "versionCode='40005'" in bad and "versionName='1.0.0-rc1'" in bad)
    check('APK display name', 'Give Me Bing and Google!' in bad)
    check('APK no requested permissions', not re.search(r'^uses-permission', bad, re.M))
    check('Debuggable candidate', 'application-debuggable' in bad)
    check('Compiled components and authority', all(v in outputs['manifest'] for v in [APP + '.InfoActivity', APP + '.FeatureProvider', APP + '.flags']))
    check('Module own SDK values', "minSdkVersion:'28'" in bad and "targetSdkVersion:'35'" in bad)
    manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
    ns = '{http://schemas.android.com/apk/res/android}'
    application = manifest.find('application')
    provider = application.find('provider')
    check('Provider authority', provider.get(ns + 'authorities') == APP + '.flags')
    check('Activity relative component', application.find('activity').get(ns + 'name') == '.InfoActivity')
    check('Description and label resources', application.get(ns + 'label') == '@string/app_name' and application.get(ns + 'description') == '@string/module_description')
    source = ROOT / 'app/src/main/java/dev/cheeseowl/gmbg'
    texts = {p.name: TEXT(p) for p in source.glob('*.java')}
    check('Migrated Java sources', len(texts) == 34 and all(t.startswith('package ' + APP + ';') for t in texts.values()))
    check('No old self namespace in sources', all('dev.qsbfix.diagnostics' not in t for t in texts.values()))
    check('Provider class agrees', f'"{APP}.flags"' in texts['FeatureProvider.java'])
    gradle = TEXT(ROOT / 'app/build.gradle.kts')
    check('Gradle identity', f'namespace = "{APP}"' in gradle and f'applicationId = "{APP}"' in gradle)
    check('API remains compileOnly', 'compileOnly(files("../vendor/libxposed-api-102.0.0.jar"))' in gradle)
    check('Pinned API Jar', SHA(ROOT / 'vendor/libxposed-api-102.0.0.jar') == 'a515dd7a53cd7a47c05e101dff77d61acb3091a97b20a885b9ea3494412db985')
    check('Pinned unchanged Wrapper Jar', SHA(ROOT / 'gradle/wrapper/gradle-wrapper.jar') == '497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7')
    target = texts['TargetSpec.java']
    check('Original QSB safety identifiers', all(v in target for v in [TARGET, '12.10.0.07203', '212100003L', '5567e02259a58b6499b121da09a37424824d419eaeb99b62520649e8302ca6e8', 'c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025']))
    check('Twenty-two business Hook specs', target.count('new Spec(') == 22)
    check('API102 runtime guard retained', 'getApiVersion()!=102' in texts['DiagnosticModule.java'])
    check('Engine/browser independent keys', all(k in texts['FeatureProvider.java'] for k in ['takeover_enabled', 'browser_enabled', 'engine_choice', 'revision']))
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        meta = {n: archive.read(n).decode('utf-8') for n in names if n.startswith('META-INF/xposed/')}
        check('Modern entry', meta.get('META-INF/xposed/java_init.list', '').strip() == APP + '.DiagnosticModule')
        check('Only QSB scope', meta.get('META-INF/xposed/scope.list', '').strip() == TARGET)
        check('API102 metadata', all(v in meta.get('META-INF/xposed/module.prop', '') for v in ['minApiVersion=102', 'targetApiVersion=102', 'staticScope=true', 'exceptionMode=protective']))
        check('No legacy entry', 'assets/xposed_init' not in names)
        dexes = [archive.read(n) for n in names if re.fullmatch(r'classes\d*\.dex', n)]
        generated = BUILD / 'intermediates/global_synthetics_dex/debug/generateDebugGlobalSynthetics/classes.dex'
        gen_bytes = generated.read_bytes()
        gen_classes = set(Dex(gen_bytes).classes)
        classes = [c for data in dexes for c in Dex(data).classes]
        check('Global synthetics tool output agrees', gen_bytes in dexes)
        check('Only module classes or tool-generated classes', all(c.startswith('Ldev/cheeseowl/gmbg/') or c in gen_classes for c in classes))
        check('Entry/provider/activity are in DEX', all('L' + APP.replace('.', '/') + '/' + name + ';' in classes for name in ['DiagnosticModule', 'FeatureProvider', 'InfoActivity']))
        check('No old package or bundled API definitions', not any(c.startswith(('Ldev/qsbfix/', 'Lio/github/libxposed/')) for c in classes))
        check('No old own namespace in DEX strings', not any('dev.qsbfix.diagnostics' in s or 'dev/qsbfix/diagnostics' in s for data in dexes for s in Dex(data).strings))
        check('No bundled research/source/libraries', not any(n.startswith(('assets/', 'lib/')) or n.endswith(('.apk', '.jar', '.aar', '.java', '.kt', '.jks', '.keystore')) for n in names))
    suites = [ET.parse(p).getroot() for p in (BUILD / 'test-results/testDebugUnitTest').glob('TEST-*.xml')]
    total = sum(int(s.get('tests', '0')) for s in suites)
    check('137 JVM tests without failures/errors/skips', total == 137 and all(all(s.get(k) == '0' for k in ['failures', 'errors', 'skipped']) for s in suites))
    lint = TEXT(BUILD / 'reports/lint-results-debug.txt')
    check('Lint zero errors', bool(re.search(r'0 errors, \d+ warnings', lint)))
    files = public_files()
    forbidden = {'.apk', '.aab', '.aar', '.dex', '.odex', '.vdex', '.so', '.class', '.jks', '.keystore', '.pem', '.key', '.p12', '.pfx', '.p8', '.asc', '.log'}
    check('No private/generated file candidates', all(p.suffix.lower() not in forbidden and p.name not in {'local.properties', '.env', 'keystore.properties', 'signing.properties', 'credentials.properties'} and not p.name.startswith('.env.') and not any(x in {'.gradle', 'build', '__pycache__'} for x in p.relative_to(ROOT).parts) for p in files))
    check('Only two approved Jar paths', {p.relative_to(ROOT).as_posix() for p in files if p.suffix == '.jar'} == {'vendor/libxposed-api-102.0.0.jar', 'gradle/wrapper/gradle-wrapper.jar'})
    sensitive = []
    for p in files:
        if p.suffix == '.jar':
            continue
        t = TEXT(p)
        if re.search(r'(?<![A-Za-z])[A-Za-z]:[\\/]|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16}', t):
            sensitive.append(p.relative_to(ROOT).as_posix())
    check('No detected absolute host paths/private keys/common token formats', not sensitive)
    cert = re.search(r'certificate SHA-256 digest: ([0-9a-f]+)', outputs['signing'])
    check('Signer certificate available', cert is not None)
    git_index = 'NOT_CREATED'
    if (ROOT / '.git').exists():
        # Local index only. Never contacts a remote or initializes/configures Git.
        index = subprocess.run(['git', '-C', str(ROOT), 'ls-files', '--cached', '-z'], capture_output=True, timeout=30)
        staged = index.stdout.decode('utf-8').rstrip('\0').split('\0') if index.stdout else []
        allowed = {p.relative_to(ROOT).as_posix() for p in files}
        check('Actual Git index only contains reviewed public candidates', index.returncode == 0 and set(staged) <= allowed)
        git_index = 'CHECKED' if index.returncode == 0 else 'UNAVAILABLE'
    result = {'status': 'LOCAL_PASS' if all(c['passed'] for c in checks) else 'LOCAL_FAIL',
              'passed': sum(c['passed'] for c in checks), 'failed': [c['check'] for c in checks if not c['passed']],
              'jvm_tests': total, 'lint': re.findall(r'\d+ errors, \d+ warnings', lint)[-1],
              'apk_sha256': SHA(apk), 'certificate_sha256': cert[1] if cert else None,
              'public_file_count': len(files), 'sensitive_file_candidates': sensitive, 'git_index': git_index,
              'verification_scope': 'Reads built artifacts and JUnit/Lint reports; does not execute Gradle',
              'runtime': 'NOT_RUN: renamed candidate', 'checks': checks}
    (OUT / 'verification.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
    print(json.dumps({k: v for k, v in result.items() if k != 'checks'}, indent=2))
    return 0 if result['status'] == 'LOCAL_PASS' else 1

if __name__ == '__main__':
    raise SystemExit(main())
