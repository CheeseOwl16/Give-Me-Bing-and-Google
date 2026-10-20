# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Cheese owl
"""Public source checks before tests/APK assembly. Standard library only; no SDK or agent."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import sys
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
CHECKS = []
TEXT = lambda p: p.read_text(encoding='utf-8-sig')
SHA = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()

def check(name, value):
    CHECKS.append({'check': name, 'passed': bool(value)})

def public_bytes(path):
    # The only admitted public binary inputs are the two pinned Jar files.
    # Text hashes represent Git's LF blobs, not Windows checkout line endings.
    data = path.read_bytes()
    return data if path.suffix == '.jar' else data.replace(b'\r\n', b'\n')


def public_files():
    return sorted(p for p in ROOT.rglob('*') if p.is_file() and
                  not any(v in {'.local', '.git'} for v in p.relative_to(ROOT).parts))


def write_manifest():
    # No network/config changes: use actual staged blobs; reject unstaged edits.
    def git(*args):
        return subprocess.check_output(['git', '-C', str(ROOT), *args])
    files = public_files()
    names = {p.relative_to(ROOT).as_posix() for p in files}
    indexed = set(git('ls-files', '-z').decode('utf-8').rstrip('\0').split('\0'))
    if indexed != names:
        raise SystemExit('Stage the exact reviewed public file set before generating hashes')
    lines = []
    for name in sorted(names - {'PUBLIC-FILES.sha256'}):
        path = ROOT / name
        blob = git('show', ':' + name)
        if blob != public_bytes(path):
            raise SystemExit('Staged blob differs from canonical working content: ' + name)
        lines.append(hashlib.sha256(blob).hexdigest() + '  ' + name + '\n')
    (ROOT / 'PUBLIC-FILES.sha256').write_text(''.join(lines), encoding='utf-8', newline='\n')
    print('Manifest generated from ' + str(len(lines)) + ' exact staged Git blobs.')


def main():
    check('LF text and binary attributes present', (ROOT / '.gitattributes').is_file() and
          all(v in TEXT(ROOT / '.gitattributes').splitlines() for v in ['* text eol=lf', '*.jar -text', '*.apk -text']))
    license_hash = 'cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30'
    check('Official complete Apache-2.0 license', SHA(ROOT / 'LICENSE') == license_hash)
    notice = TEXT(ROOT / 'NOTICE')
    check('Public author and license scope', 'Copyright 2026 Cheese owl' in notice and 'Third-party' in notice)
    check('Existing third-party license texts retained', all((ROOT / p).is_file() for p in ['LICENSES/Apache-2.0.txt', 'LICENSES/Gradle-Wrapper.txt', 'ACKNOWLEDGEMENTS.md']))
    check('Pinned API Jar', SHA(ROOT / 'vendor/libxposed-api-102.0.0.jar') == 'a515dd7a53cd7a47c05e101dff77d61acb3091a97b20a885b9ea3494412db985')
    check('Pinned Wrapper Jar', SHA(ROOT / 'gradle/wrapper/gradle-wrapper.jar') == '497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7')
    wrapper = TEXT(ROOT / 'gradle/wrapper/gradle-wrapper.properties')
    check('Wrapper distribution verified', 'gradle-9.5.0-bin.zip' in wrapper and 'distributionSha256Sum=553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746' in wrapper)
    main_java = list((ROOT / 'app/src/main/java/dev/cheeseowl/gmbg').glob('*.java'))
    test_java = list((ROOT / 'app/src/test/java/dev/cheeseowl/gmbg').glob('*.java'))
    check('All original source/test units present', len(main_java) == 35 and len(test_java) == 7)
    check('All Java units use GMBG namespace', all(TEXT(p).startswith('package dev.cheeseowl.gmbg;') for p in main_java + test_java))
    check('All 149 test methods retained', sum(len(re.findall(r'@Test\b', TEXT(p))) for p in test_java) == 149)
    target = TEXT(ROOT / 'app/src/main/java/dev/cheeseowl/gmbg/TargetSpec.java')
    check('Hook scope/version pins unchanged', all(v in target for v in ['com.android.quicksearchbox', '12.10.0.07203', '212100003L', '5567e02259a58b6499b121da09a37424824d419eaeb99b62520649e8302ca6e8']) and target.count('new Spec(') == 22)
    props = TEXT(ROOT / 'app/src/main/resources/META-INF/xposed/module.prop')
    check('Modern API102 metadata', all(v in props for v in ['minApiVersion=102', 'targetApiVersion=102', 'exceptionMode=protective']))
    check('Modern entry and scope', TEXT(ROOT / 'app/src/main/resources/META-INF/xposed/java_init.list').strip() == 'dev.cheeseowl.gmbg.DiagnosticModule' and TEXT(ROOT / 'app/src/main/resources/META-INF/xposed/scope.list').strip() == 'com.android.quicksearchbox')
    manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
    ns = '{http://schemas.android.com/apk/res/android}'
    check('Read-only provider identity', manifest.find('application/provider').get(ns + 'authorities') == 'dev.cheeseowl.gmbg.flags')
    check('No network or other requested permissions', not manifest.findall('uses-permission'))
    own_gradle = TEXT(ROOT / 'app/build.gradle.kts')
    check('Candidate identity preserved', all(v in own_gradle for v in ['namespace = "dev.cheeseowl.gmbg"', 'applicationId = "dev.cheeseowl.gmbg"', 'versionCode = 40006', 'versionName = "1.1.0-rc1"']))
    check('CI SDK/build tool values match source', 'minorApiLevel = 1' in own_gradle and 'release(36)' in own_gradle and 'buildToolsVersion = "36.0.0"' in own_gradle)
    workflow = TEXT(ROOT / '.github/workflows/android-debug.yml')
    check('CI pins three official Actions', all(p in workflow for p in [
        'actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1',
        'actions/setup-java@b843ebf9b913146b38da2eaa7aeddf76074ae1ad',
        'actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9']))
    check('CI trigger/permission/runner policy', all(p in workflow for p in ['workflow_dispatch:', 'branches: [main]', 'contents: read', 'runs-on: ubuntu-24.04', "java-version: '21'"]))
    check('CI tests before assembly', workflow.index(':app:testDebugUnitTest') < workflow.index(':app:assembleDebug'))
    check('CI no private agent/secrets or ignored failures', not any(p in workflow for p in ['javaagent', 'HostAgent', 'secrets.', 'continue-on-error', 'always()']))
    check('CI artifact fixed named files only', '.local/ci-artifact/GMBG-Debug.apk' in workflow and
          'retention-days: 14' in workflow and 'if-no-files-found: error' in workflow and
          not any(p in workflow.split('          path: |', 1)[1].split('          include-hidden-files:', 1)[0] for p in ['*', '.log', 'keystore', 'local.properties']))
    files = public_files()
    forbidden = {'.apk', '.aab', '.aar', '.dex', '.odex', '.vdex', '.so', '.class', '.jks', '.keystore', '.pem', '.key', '.p12', '.pfx', '.p8', '.asc', '.log'}
    check('No private/generated candidate files', all(p.suffix.lower() not in forbidden and p.name not in {'local.properties', '.env', 'keystore.properties', 'signing.properties', 'credentials.properties'} and not p.name.startswith('.env.') and not any(v in {'build', '.gradle', '__pycache__'} for v in p.relative_to(ROOT).parts) for p in files))
    check('Only approved Jar inputs', {p.relative_to(ROOT).as_posix() for p in files if p.suffix == '.jar'} == {'vendor/libxposed-api-102.0.0.jar', 'gradle/wrapper/gradle-wrapper.jar'})
    secret_pattern = r'(?<![A-Za-z])[A-Za-z]:[\\/]|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16}'
    check('No detected host paths or common secret forms', all(not re.search(secret_pattern, TEXT(p)) for p in files if p.suffix != '.jar'))
    expected = {}
    for line in TEXT(ROOT / 'PUBLIC-FILES.sha256').splitlines():
        digest, name = line.split('  ', 1)
        expected[name] = digest
    names = {p.relative_to(ROOT).as_posix() for p in files} - {'PUBLIC-FILES.sha256'}
    check('Reviewed public file set complete', set(expected) == names)
    mismatches = [n for n, h in expected.items() if not (ROOT / n).is_file() or
                  hashlib.sha256(public_bytes(ROOT / n)).hexdigest() != h]
    check('Reviewed public file hashes match', not mismatches)
    ignore = TEXT(ROOT / '.gitignore')
    check('Private outputs and signing material ignored', all(v in ignore for v in ['/.local/', '/.gradle/', '**/build/', '/local.properties', '*.keystore', '*.jks', '*.p12', '*.pem', '*.key']))
    result = {'status': 'LOCAL_PASS' if all(c['passed'] for c in CHECKS) else 'LOCAL_FAIL',
              'passed': sum(c['passed'] for c in CHECKS), 'failed': [c['check'] for c in CHECKS if not c['passed']], 'checks': CHECKS, 'hash_mismatches': mismatches}
    out = ROOT / '.local'
    out.mkdir(exist_ok=True)
    (out / 'preflight.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
    print(json.dumps({k: v for k, v in result.items() if k != 'checks'}, indent=2))
    return 0 if result['status'] == 'LOCAL_PASS' else 1

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write-manifest', action='store_true',
                        help='Generate hashes from the exact staged LF Git blobs; stage edits first')
    args = parser.parse_args()
    if args.write_manifest:
        write_manifest()
    else:
        sys.exit(main())
