# GitHub Actions：只生成 Debug Artifact

工作流：`.github/workflows/android-debug.yml`，名称 **Android Debug**。支持 `workflow_dispatch` 手动触发和 `main` push；没有 PR 特权触发、自动版本修改、源码回写或 Release 发布。

## 环境与顺序

GitHub 托管 `ubuntu-24.04`，JDK21 / Temurin，项目 Wrapper9.5.0、AGP9.3.3。官方 runner 的 Android command-line tools 通过 sdkmanager 接受 SDK 许可并确保 `platforms;android-36.1`、`build-tools;36.0.0` 已安装。无需 QSB APK、DEX、私有研究材料、本机路径或私人 agent。

执行顺序严格为：

1. 检出源码，禁止持久保存 checkout 凭据；设置 JDK21。
2. `scripts/preflight.py` 检查项目许可、公开清单/hash、固定 Jar、模块身份、源码/测试和敏感文件。
3. `scripts/prepare_ci_sdk.py` 使用 runner 已提供的官方 sdkmanager；没有远程 shell 下载执行，没有设备命令。
4. Wrapper 执行全部 `testDebugUnitTest` 和 `lintDebug`。
5. 上述成功后执行 `assembleDebug`。
6. `scripts/verify.py` 做 APK 结构、签名、API102 元数据、测试/Lint 和公开文件复核。
7. `scripts/package_debug.py` 仅暂存 APK、SHA256 和公共许可通知；全部成功才上传 Artifact。

任一步失败即停止，不删除测试、不跳过失败、不上传失败日志或环境快照。SDK 工具详细日志仅留 runner 的 `.local/`；Artifact 不包含它们。权限仅 `contents: read`，无 PAT/用户 Secret；Actions 自带的运行时凭据不写入项目或 Artifact。

## 固定官方 Actions

2026-10-20 匿名核对官方 release/tag 对象，完整 Commit 固定：

| Action | 版本 | Commit |
|---|---|---|
| [actions/checkout](https://github.com/actions/checkout/releases/tag/v7.0.1) | v7.0.1 | `3d3c42e5aac5ba805825da76410c181273ba90b1` |
| [actions/setup-java](https://github.com/actions/setup-java/releases/tag/v6.1.0) | v6.1.0 | `b843ebf9b913146b38da2eaa7aeddf76074ae1ad` |
| [actions/upload-artifact](https://github.com/actions/upload-artifact/releases/tag/v7.0.2) | v7.0.2 | `cf430e030ddbb5b0abf93d22962f4752f3646cd9` |

更新时人工审阅上游版本、输入和安全变化，再更新 Commit 与本文件；不自动跟随浮动 major 标签。JDK21 补丁和 Ubuntu 镜像仍会更新，不声称完整二进制可复现。

官方依据：[Ubuntu24.04 runner 软件清单](https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2404-Readme.md)、[官方 SDK 安装脚本](https://github.com/actions/runner-images/blob/main/images/ubuntu/scripts/build/install-android-sdk.sh)。这些资料说明路径可行，不证明本项目 CI 已执行。

## Artifact 与签名

名字为 `GMBG-Debug-运行号-尝试号`，保留配置 14 天；下载的是 ZIP，内有 `GMBG-Debug.apk`、SHA256SUMS、LICENSE、NOTICE、致谢和许可文本。上传路径是八项明确文件列表；虽然 `.local` 为隐藏目录而设置 include-hidden-files=true，也不会上传整个 `.local/`。

采用 AGP 正常 Debug 签名。没有自建/持久维护 Release keystore，不缓存或上传 Debug keystore/password。临时 runner 的不同运行可能具有不同 Debug 证书，不能保证覆盖升级；不兼容时用户可能需要卸载再安装并丢失配置。Artifact 不是正式稳定发行版，也不创建 GitHub Release。

## 真实验证边界

- LOCAL_PASS：本机普通 JVM、不带任何 agent，直接 JUnitCore 执行全部 137 项测试通过；测试源码/业务 Java 未修改。
- 本机标准 Gradle、不带 agent：失败 `Unable to establish loopback connection`，发生于 Gradle 启动/IPC 阶段，尚未执行测试。这不是测试断言失败。
- Docker daemon 不可用，WSL 访问受限，未通过本地 Linux 环境模拟 runner。没有启动 Docker 服务、虚拟机或 Android 模拟器。
- 工作流 YAML 已本地解析，官方 Action 输入和权限/顺序/Artifact 白名单已静态核对。辅助解析器只在本地使用；CI 源码检查仍为 Python 标准库，无新的公开 PyYAML 依赖。
- **GitHub 托管 CI：NOT_RUN**。Ubuntu 不带 agent 的完整编译/测试尚无运行证据；首次真实运行可能暴露 SDK 仓库、AGP、网络或 runner 差异，必须如实处理，不能视 YAML 有效为成功。
- 40005 / `dev.cheeseowl.gmbg` 已由用户手动确认正常工作（**USER_REPORTED**）：Xiaomi 15 Pro / `haotian` / HyperOS `OS3.0.309` / Android 16 / QSB `12.10.0.07203`。该反馈不代表其他设备或所有回归项目均已验证。

工作流已经有正规的公开构建路径，但没有证明它必然成功。首次上传后需由作者观察完整成功运行，才能把该次 CI 标为 PASS。
