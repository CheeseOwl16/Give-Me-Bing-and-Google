# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Cheese owl
"""Stage only Debug APK, checksum and public notices; never stage logs or keys."""
from pathlib import Path
import hashlib
import json
import shutil

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / '.local/ci-artifact'
LEGAL = ['LICENSE', 'NOTICE', 'ACKNOWLEDGEMENTS.md',
         'LICENSES/Apache-2.0.txt', 'LICENSES/Gradle-Wrapper.txt']

def main():
    verified = json.loads((ROOT / '.local/verification.json').read_text(encoding='utf-8'))
    if verified['status'] != 'LOCAL_PASS':
        raise SystemExit('Completed local verification must pass before packaging')
    apk = ROOT / '.local/build/app/outputs/apk/debug/app-debug.apk'
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    if digest != verified['apk_sha256']:
        raise SystemExit('APK changed after verification')
    allowed = set(LEGAL + ['GMBG-Debug.apk', 'SHA256SUMS.txt', 'README.txt'])
    if OUT.exists():
        unexpected = [p.relative_to(OUT).as_posix() for p in OUT.rglob('*')
                      if p.is_file() and p.relative_to(OUT).as_posix() not in allowed]
        if unexpected:
            raise SystemExit('Unexpected files in artifact staging directory; no automatic cleanup is performed')
    OUT.mkdir(parents=True, exist_ok=True)
    for name in LEGAL:
        destination = OUT / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(ROOT / name, destination)
    shutil.copy2(apk, OUT / 'GMBG-Debug.apk')
    (OUT / 'SHA256SUMS.txt').write_text(digest + '  GMBG-Debug.apk\n', encoding='ascii')
    (OUT / 'README.txt').write_text(
        'GMBG Debug APK, not a stable signed Release.\n'
        'Different CI runs may have incompatible debug signing certificates.\n'
        'Reinstallation may require removing the previous app and can lose settings.\n'
        'Configure the new module scope manually; do not enable the old and new identities together.\n'
        'Source and compatibility details are in the project repository README.\n'
        'License and notices accompany this APK. Device acceptance is not performed by CI.\n', encoding='utf-8')
    print('Debug artifact staged: APK, checksum and public license notices only.')

if __name__ == '__main__':
    main()
