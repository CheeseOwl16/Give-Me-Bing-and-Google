# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Cheese owl
"""Use the hosted Ubuntu runner's official sdkmanager. No device tools are run."""
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / '.local'

def main():
    if os.name == 'nt':
        raise SystemExit('CI SDK preparation is for the hosted Ubuntu job; configure your local SDK separately.')
    value = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not value:
        raise SystemExit('Hosted runner must provide ANDROID_HOME or ANDROID_SDK_ROOT')
    sdk = Path(value)
    manager = sdk / 'cmdline-tools/latest/bin/sdkmanager'
    if not manager.is_file():
        raise SystemExit('Official Android command-line tools are missing from the hosted runner')
    OUT.mkdir(exist_ok=True)
    commands = [([str(manager), '--licenses'], 'y\n' * 100),
                ([str(manager), 'platforms;android-36.1', 'build-tools;36.0.0'], None)]
    with (OUT / 'ci-sdk.log').open('w', encoding='utf-8') as log:
        for command, answers in commands:
            run = subprocess.run(command, input=answers, capture_output=True, text=True,
                                 encoding='utf-8', errors='replace', timeout=600)
            log.write(run.stdout + run.stderr)
            if run.returncode:
                print('\n'.join((run.stdout + run.stderr).splitlines()[-25:]))
                raise SystemExit('Official sdkmanager failed; no tests or APK build have been marked successful')
    if not (sdk / 'platforms/android-36.1/android.jar').is_file():
        raise SystemExit('Required SDK platform is unavailable')
    if not (sdk / 'build-tools/36.0.0/aapt2').is_file():
        raise SystemExit('Required SDK build tools are unavailable')
    print('Required Android platform and build tools are installed; no device operation was performed.')

if __name__ == '__main__':
    main()
