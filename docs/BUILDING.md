# 构建及本地检查

## 固定配置

| 配置 | 当前值 |
|---|---|
| JDK | 本地验证使用 21；Java source/target 17 |
| Wrapper / AGP | 9.5.0 / 9.3.3 |
| Android SDK | platform 36.1，Build Tools 36.0.0 |
| 模块 SDK | min 28，target 35，compile 36.1 |
| 编译 API | 现代 libxposed 102.0.0，compileOnly 固定摘要 Jar |
| JVM 测试 | JUnit 4.13.2，传递依赖 Hamcrest Core 1.3 |
| 版本 | 1.0.0-rc1 / 40005，Debug |

设置 `JAVA_HOME` 指向自己的 JDK，`ANDROID_HOME` 或 `ANDROID_SDK_ROOT` 指向自己的 SDK。不要提交 `local.properties`、用户 Gradle 缓存或密钥。工程没有固定用户名、SDK/JDK/Gradle 安装路径，也不读取外部 APK、DEX 或研究档案。

## 常规构建

Windows PowerShell：

```powershell
./scripts/build.ps1 -Offline
```

脚本校验 API Jar、调用本仓库 Wrapper，执行 `assembleDebug`、`testDebugUnitTest`、`lintDebug`。无缓存时去掉 `-Offline`，由公开的 Google Maven/Maven Central/Gradle 源下载依赖；离线模式不意味着依赖随源码完整分发。

其他平台：

```sh
sh ./gradlew --project-cache-dir .local/gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Wrapper properties 含 Gradle 分发 SHA-256。所有 Gradle 项目 build 输出定向到 `.local/build/`，项目缓存通过上述入口定向到 `.local/gradle/`。不要运行安装、connected、managedDevice 或模拟器任务；工程保留了设备任务门禁。

## 本地检查

安装 Python3 后：

```sh
python scripts/verify.py
```

使用同一 SDK/JDK 环境，或显式传 `--sdk` 和 `--java`。只读取公开源码、当前构建输出及本机 SDK 工具；不依赖私有验证证据。检查输出为 `.local/verification.json`，原始静态工具输出在 `.local/checks/`。此脚本不运行设备工具。

常见产物：

```text
.local/build/app/outputs/apk/debug/app-debug.apk
.local/build/app/test-results/testDebugUnitTest/
.local/build/app/reports/lint-results-debug.txt
.local/build.log
.local/verification.json
```

这些都是本地生成内容，不属于待提交源码。Debug 签名由使用者自己的 Android Debug keystore 提供；不提供、不生成正式 Release 私钥。不同用户的 Debug 签名和输出哈希可能不同。

项目已选择由 GitHub Actions 自动生成 Debug Artifact，不维护专用 Release keystore，也不创建 GitHub Release。临时 runner 的不同运行可能用不同 Debug 证书；无法保证覆盖安装，卸载重装可能丢失设置。仓库和 Artifact 均不得包含 keystore/password。

自有源码采用 Apache-2.0，完整 LICENSE/NOTICE 及第三方说明随 Artifact 附带。流水线、下载和验证边界见 [CI](CI.md)。

## 本机可选兼容参数

本轮宿主环境构建使用了既有 JVM Unix-domain-socket 兼容 agent，API Jar 和业务代码不依赖它。该 agent 的来源和适用环境不适合作为通用项目依赖，因此没有复制源码或二进制到候选。

仅在使用者自行持有并信任相应主机工具时，可执行 `./scripts/build.ps1 -Offline -HostAgent <trusted-agent-path>`。脚本只在本次进程设置/恢复 JAVA_OPTS，并给 Gradle/Test JVM 显式传参数，不改系统环境变量。不要下载或执行未知 agent。这是既有本地可选接口，GitHub Actions 不传入它。

第二轮本机标准 Wrapper 不带 agent，在 Gradle 启动/IPC 阶段出现 `Unable to establish loopback connection`，尚未执行测试；不能记为测试断言失败，也不能记 testDebugUnitTest PASS。使用第一轮已编译且源码未变的测试类，普通 JVM/JUnitCore 不带 agent 直接运行 137 项测试通过，这是独立的补充验证，不替代 Gradle 任务或干净 Ubuntu 验证。没有安装 Docker、启用 WSL 或修改 Windows 系统配置。

## 验证范围

本次本地构建成功不等于可复现构建认证：未在干净第二环境重建、未验证 Linux/macOS、未承诺二进制逐字节相同。SDK/Gradle/JDK 和已缓存依赖仍是外部条件。项目没有依赖专有研究输入的编译步骤。

工作流先执行 `scripts/preflight.py`（标准库，无 SDK/解析器依赖），再运行全部 Gradle JVM 测试和 Lint；成功后 assembleDebug、运行完整 verify.py、暂存并上传精确白名单 Artifact。任何失败都会阻断后续，不跳过测试。公开文件改动后必须重新生成 PUBLIC-FILES.md/SHA256 清单再做 preflight。

改包后的设置、Provider 通信、模块识别和作用域需要人工实机回归。Debug 不是正式稳定发行版；GitHub 实际 CI 与 Ubuntu 无 agent 构建仍为 NOT_RUN。
