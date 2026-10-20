# Give Me Bing and Google!

**Use Google or Bing in Xiaomi Search, and open web results with your default browser.**

GMBG (Give Me Bing and Google!) is an LSPosed module for Xiaomi's built-in Search app.

The idea behind this project is simple: I wanted to choose my own search engine and decide which browser opens my web searches.

简体中文 · Build Instructions · Acknowledgements

## Features

- **Google / Bing selection** — Choose Google or Bing for ordinary web searches.

- **Default browser support** — Open supported web searches with your Android default browser instead of being forced into Xiaomi Browser.

- **Independent switches** — Enable or disable search engine takeover and browser redirection separately.

- **Native search preserved** — Local apps, photos, contacts, files, and other on-device search features continue to work normally.

- **Search history support** — Handles supported web suggestions and search history paths while preserving native behavior such as direct URL navigation.

GMBG is not intended to replace Xiaomi Search. When a request does not meet the takeover conditions, the module attempts to preserve the original behavior.

## Compatibility

The module has been manually tested in the following environment:

| Item               | Tested environment           |
| ------------------ | ---------------------------- |
| Device             | Xiaomi 15 Pro                |
| Device codename    | `haotian`                    |
| Firmware           | HyperOS `OS3.0.309`          |
| Android            | Android 16                   |
| Target application | Xiaomi Search                |
| Target package     | `com.android.quicksearchbox` |
| Target version     | `12.10.0.07203`              |
| versionCode        | `212100003`                  |
| Hook API           | libxposed API 102            |

A compatible LSPosed framework supporting the modern **libxposed API 102** is required.

GMBG performs strict version, signature, and method checks against the target Xiaomi Search component. Other devices, firmware versions, or Search app versions may not work.

Broad compatibility testing has not been performed.

## Download and Installation

Debug APKs are built automatically using GitHub Actions.

1. Open the repository's **Actions** tab.

2. Select the **Android Debug** workflow.

3. Open a successful workflow run.

4. Download the `GMBG-Debug-...` ZIP under **Artifacts**.

5. Extract `GMBG-Debug.apk`.

6. Install the APK and enable the module in LSPosed.

7. Select only Xiaomi Search (`com.android.quicksearchbox`) as its scope.

8. Open GMBG, choose Google or Bing, and configure the two feature switches.

If the workflow has not completed successfully yet, the APK will not be available for download.

**Note:** GitHub Actions uses standard Android Debug signing. Different workflow runs may produce APKs signed with different certificates, preventing in-place upgrades. You may need to uninstall the previous APK and reinstall it, then configure the module and LSPosed scope again.

Artifacts are configured to be retained for 14 days, subject to GitHub's actual retention policies.

## About This Project

This is a small project created to solve a personal problem.

**All program code in this project was implemented using AI coding tools, primarily OpenAI Codex.**

The human contributor only provided ideas, feature requests, bug reports, manual testing on real devices, and project organization and publication. Code implementation, build scripts, and technical fixes were handled by AI.

The development process mostly consisted of describing what I wanted, testing the results, reporting problems, and asking AI to improve the implementation.

In that sense, this project is also an experiment in AI-driven software development.

GMBG primarily exists for my own use. There is no long-term maintenance plan. I may update it occasionally based on personal interest or need, but compatibility with other devices, future system versions, and ongoing technical support are not guaranteed.

Feel free to use, study, modify, and build upon the source code.

## Privacy

GMBG does not request network permission, upload search history, or send telemetry.

It stores only the configuration needed for its feature switches and search engine selection. Diagnostic logs use status identifiers and call correlation data rather than full search queries or web URLs.

Web requests are still handled by the browser.

## Building from Source

GMBG is written in Java and uses Gradle with the modern libxposed API 102.

Main build requirements:

- JDK 21

- Gradle 9.5.0

- Android Gradle Plugin 9.3.3

- Android SDK 36.1

- Android Build Tools 36.0.0

On Windows:

```
./scripts/build.ps1
```

Or use the Gradle Wrapper directly:

```
./gradlew :app:assembleDebug
```

See BUILDING.md for detailed instructions.

The GitHub Actions workflow is located at `.github/workflows/android-debug.yml`. It runs automated checks and builds Debug APKs without committing generated binaries to the source branch.

## Acknowledgements

The development of this project was informed by ideas from the following open-source projects:

- [Hyper Helper / XiaomiHelper](https://github.com/HowieHChen/XiaomiHelper) — Search engine extension concepts for Xiaomi systems.

- [Fxxk-MiBrowser](https://github.com/DuhMatt/Fxxk-MiBrowser) — Default browser redirection concepts.

- [libxposed-api](https://github.com/JingMatrix/libxposed-api) — Modern Xposed Hook API.

Thanks to the authors and contributors of these projects.

For details about references and third-party licenses, see ACKNOWLEDGEMENTS.md.

## License

Copyright 2026 Cheese owl

Project-owned source code and documentation are licensed under the Apache License 2.0. Third-party materials remain subject to their respective licenses. See NOTICE and LICENSES.

This project is not affiliated with Xiaomi, Google, Microsoft, Chrome, or their respective owners.
