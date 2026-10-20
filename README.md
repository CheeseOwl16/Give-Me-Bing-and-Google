# Give Me Bing and Google!

**让小米搜索使用 Google 或 Bing，并通过系统默认浏览器打开网页。**

GMBG（Give Me Bing and Google!）是一个针对小米系统「搜索」组件的 LSPosed 模块。

这个项目的起因很简单：我希望自己决定使用哪个搜索引擎，以及用什么浏览器打开搜索结果。

[English](README_EN.md) · [构建说明](docs/BUILDING.md) · [第三方致谢](ACKNOWLEDGEMENTS.md)

## 功能

- **Google / Bing 搜索引擎切换**：可以自由选择使用 Google 或 Bing 进行普通网页搜索。

- **系统默认浏览器**：让网页搜索使用 Android 系统默认浏览器，而非强制打开小米浏览器。

- **独立开关**：搜索引擎接管和浏览器重定向可分别启用或关闭。

- **保留本机搜索**：本地应用、相册、联系人、文件等搜索功能保持原生行为。

- **兼容搜索历史**：支持已适配的网页搜索建议和搜索历史路径，同时保留网址直达等原生操作。

本模块不会试图替代整个小米搜索。对于不满足接管条件的操作，会尽可能保留原有行为。

## 兼容性

目前已在以下环境完成实机测试：

| 项目          | 测试环境                         |
| ----------- | ---------------------------- |
| 设备          | Xiaomi 15 Pro                |
| 设备代号        | `haotian`                    |
| 系统          | HyperOS `OS3.0.309`          |
| Android     | Android 16                   |
| 目标应用        | 小米搜索                         |
| 目标包名        | `com.android.quicksearchbox` |
| 目标版本        | `12.10.0.07203`              |
| versionCode | `212100003`                  |
| Hook API    | libxposed API 102            |

需要支持现代 **libxposed API 102** 的 LSPosed 兼容框架。

模块包含针对目标搜索组件的版本、签名和方法校验，因此其他设备、固件或搜索组件版本不一定能够正常工作。

目前没有对其他设备进行广泛适配测试。

## 下载与安装

本项目通过 GitHub Actions 提供自动构建的 Debug APK。

1. 打开仓库的 **Actions** 页面。

2. 选择 **Android Debug** 工作流。

3. 打开一次构建成功的运行记录。

4. 在 **Artifacts** 中下载 `GMBG-Debug-...` ZIP。

5. 解压得到 `GMBG-Debug.apk`。

6. 安装 APK，并在 LSPosed 中启用模块。

7. 作用域仅选择小米「搜索」`com.android.quicksearchbox`。

8. 打开 GMBG，选择 Google 或 Bing，并按需要启用两个功能开关。

如果 Actions 尚未成功运行，需要等待首次构建完成后才能下载 APK。

**注意：** GitHub Actions 使用普通 Debug 签名。不同构建之间可能存在签名不兼容，导致无法直接覆盖安装。必要时需要卸载后重新安装，并重新配置模块及 LSPosed 作用域。

构建产物的保留期限为 14 天，实际可用时间以 GitHub 平台规则为准。

## 关于这个项目

这是一个由个人使用需求催生的小项目。

**本项目的全部程序代码均由 AI 编程工具实现，主要使用 OpenAI Codex。**

真人只负责提供想法、提出功能需求、反馈 Bug、进行实机测试，以及整理和发布项目。代码实现、构建脚本和技术问题修复均交由 AI 完成。

整个开发过程基本就是不断提出需求、测试结果、反馈问题，再让 AI 修改实现。

这也是一次 AI 编程的实践。

项目主要为了满足我自己的使用需求，没有长期维护计划。以后可能会根据兴趣或实际需要更新，但不保证适配其他设备、后续系统版本，也不承诺持续提供技术支持。

欢迎使用、研究和修改源码。

## 隐私

GMBG 不需要网络权限，也不会主动上传搜索记录或发送遥测数据。

模块只保存必要的功能开关和搜索引擎选择。诊断日志使用状态标识及调用关联信息，不记录完整搜索词或网页 URL。

实际网页搜索仍由浏览器处理。

## 从源码构建

项目使用 Java、Gradle 和现代 libxposed API 102。

主要构建环境：

- JDK 21

- Gradle 9.5.0

- Android Gradle Plugin 9.3.3

- Android SDK 36.1

- Android Build Tools 36.0.0

Windows 下可执行：

```
./scripts/build.ps1
```

也可以通过 Gradle Wrapper 构建：

```
./gradlew :app:assembleDebug
```

详细构建方式请参阅 [BUILDING.md](docs/BUILDING.md)。

GitHub Actions 工作流位于 [.github/workflows/android-debug.yml](.github/workflows/android-debug.yml)，用于自动测试和生成 Debug APK，不会将生成的 APK 提交到源码分支。

## 致谢

开发过程中参考了以下开源项目的设计思路：

- [Hyper Helper / XiaomiHelper](https://github.com/HowieHChen/XiaomiHelper) — 小米系统搜索引擎扩展相关思路。

- [Fxxk-MiBrowser](https://github.com/DuhMatt/Fxxk-MiBrowser) — 默认浏览器跳转处理相关思路。

- [libxposed-api](https://github.com/JingMatrix/libxposed-api) — 现代 Xposed Hook API。

感谢这些项目的作者和贡献者。

具体来源及第三方许可证信息请参阅 [ACKNOWLEDGEMENTS.md](ACKNOWLEDGEMENTS.md)。

## 开源许可证

Copyright 2026 Cheese owl

项目自有源码及文档采用 [Apache License 2.0](LICENSE) 开源。第三方材料仍遵循各自许可证，详见 [NOTICE](NOTICE) 和 [LICENSES](LICENSES/)。

本项目与 Xiaomi、Google、Microsoft、Chrome 等相关厂商没有官方关联。
