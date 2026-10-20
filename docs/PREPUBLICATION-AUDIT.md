# 首次公开发布前审核

审核日期：2026-10-20。版本为 1.0.0-rc1 / 40005。本次状态清理仅修改文档和一处过时设置文字，不修改业务代码。

## 授权与署名

- 自有源码和文档已按用户决定采用 **Apache-2.0**，公开署名 **Cheese owl**。根 LICENSE 是 Apache 官方完整文本，SHA-256 `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30`。
- 根 NOTICE 是项目署名/范围通知，内容不修改许可。现存第三方没有发现要求合并进根目录的独立 NOTICE；选择创建简短根 NOTICE 便于随 Debug Artifact 保留署名，不称其为所有 Apache 项目必需。
- 新工作流和自有辅助脚本使用 SPDX 标识；冻结的 Java 不为添加头部而修改。第三方 Wrapper、API 和 LICENSES 文本原样保留，没有把上游代码改署名。
- Hyper Helper 固定根许可为 GPLv3，具体 or-later 未确认；Fxxk 为 MIT。此前未发现直接复用实现，但这不是完全无版权风险的证明。Apache 只覆盖作者有权许可部分；若发现实际 GPL 衍生或来源不明材料，停止相应发行并重新审查。见 ACKNOWLEDGEMENTS。

## 功能保护与文档

34 个主 Java 文件和 6 个测试文件未改；设置文字仅移除过时的“尚未实机验证”说明。Gradle 配置、API Jar、Wrapper 及第三方许可文件也未改变。22 个 Hook、包名、API102 初始化、Google/Bing 模板/编码、历史域、浏览器路由、开关和失败回退保持。

README 中英文已更新为 AI 代码/脚本实现与真人需求、设备、测试、反馈、验收和许可决策分工；没有虚构人工代码审查或专业安全审计。个人维护政策不承诺长期更新、未来兼容、所有 Bug 修复或响应时限，不增加 Apache 使用限制。

40005 / `dev.cheeseowl.gmbg` 已由用户手动确认正常工作（**USER_REPORTED**）：Xiaomi 15 Pro / `haotian` / HyperOS `OS3.0.309` / Android 16 / QSB `12.10.0.07203`。该反馈不代表其他设备或所有回归项目均已验证。目标 versionCode 为 212100003，框架需要现代 libxposed API102。

## 真实验证结果与限制

| 项目 | 结果及范围 |
|---|---|
| 第一轮本地 Gradle Debug/JVM/Lint | 历史 LOCAL_PASS，使用既有可选主机 agent；137 测试，Lint 0 errors/11 warnings；记录已保留 |
| 第二轮标准 Wrapper，不带 agent | **FAILED_ENV**：`Unable to establish loopback connection`，发生于启动/IPC；测试尚未执行，不是断言失败，不记 testDebugUnitTest PASS |
| 第二轮普通 JVM/JUnitCore，不带 agent | **LOCAL_PASS，137 项**；使用第一轮已编译且源码未变的测试类，是补充验证，不替代 Gradle 任务 |
| 第二轮 APK 结构和完整静态复核 | LOCAL_PASS，39 项检查；读取第一轮构建产物/JUnit/Lint 报告，检查器本身不执行 Gradle |
| 新源码 preflight | LOCAL_PASS，27 项；标准库脚本检查许可/公开清单/hash/源码/CI 策略，实际结果保存在本地 JSON，CI 在 APK 构建前执行 |
| 工作流 YAML | 已有本地语法解析与 17 项策略核对记录；官方 Action 输入按固定 Commit 元数据检查；不是托管执行 |
| Docker / WSL 模拟 Ubuntu | 不可用/访问受限；没有安装、启用或改系统配置，没有本地 Ubuntu 验证 |
| GitHub Actions 实际运行 | **NOT_RUN**，尚未上传仓库；不宣称 CI 已成功 |
| 候选 APK | 仍为第一轮 Debug 产物，SHA-256 `5fa433d9cd96885ce61027f0913b19b8f22529ac1eed32453ed1b29c03781f03`；本次文字资源更新后未重建 APK 或改签 |

无 agent 的 Gradle 启动失败与普通 JVM 测试通过分别记录；没有删测试、隐藏失败、放宽安全校验或修改 Java。已有本地语法记录与 GitHub 运行状态分开。

## CI 和二进制分发

`.github/workflows/android-debug.yml` 只在 main push/手动时运行，Ubuntu24.04、JDK21、Wrapper 和官方 SDK。先公开检查、全部 JVM/Lint，再 assembleDebug、APK 校验和 Artifact。无私人 host agent、PAT、Secret、远程 shell 下载执行、版本修改、源码提交或 Release。

权限为 contents: read。官方 Actions 使用完整 Commit 固定，版本和官方来源见 CI.md。Artifact 名称 GMBG-Debug-运行号-尝试号、保留配置14天，上传仅八项固定文件：APK、摘要及公共许可/通知/说明；不上传环境、原始日志、密钥或缓存。下载规则及期限仍受 GitHub 政策/可见性影响。

按 AGP 正常 Debug 签名，可能由临时 runner 自动产生临时 Debug 证书。没有专用长期 Release keystore，也不缓存/上传 Debug 密钥。不同运行可能不兼容覆盖安装，卸载重装可丢配置；安装后需正确配置作用域，避免多个同类模块重复干预。

## 公开文件与敏感内容

PUBLIC-FILES.md / PUBLIC-FILES.sha256 是拟提交集合和摘要，共 81 个公开文件、80 条摘要，SHA 文件不对自身求摘要。本目录已初始化 Git 仓库，分支为 main，81 项公开文件已暂存；尚无 Commit 或远端，也没有执行 Push。摘要更新后复核实际暂存内容与磁盘一致。

候选从最小白名单建立，没有先拷贝整个工作区再靠 ignore 隐藏。`.local/` 仅用于本地生成内容，全部排除；不要压缩或拖拽整个工作目录上传。公开集合不含 QSB APK/DEX/SO、JADX/反汇编、私有研究、设备日志、local.properties、缓存或私钥。两项获许可 Jar 仍是唯一公开二进制构建输入。

文件类型、路径、文本和常见 token/私钥格式复核不等于所有秘密形式的绝对证明。兼容方法签名/目标摘要是必要接口事实，不包含用户搜索记录。SDK/R8 生成支持定义的说明保持，不把 APK 宣传成只有自有代码，也不将此次来源复核称作专业安全审计。

## 可公开状态及待确认

已解决项目 LICENSE 待定问题；本轮未发现新增许可证冲突或需要阻断源码公开的敏感文件。**可以由作者手动准备首次公开提交，GitHub Actions 实际运行仍为 NOT_RUN。** 这不是对所有版权风险的法律证明，也不代表未经用户确认的测试范围已经通过。

仍需作者：首次提交前核对 Git 索引；手动首次上传并观察完整 CI；按需完成其他回归项目；持续核对未来第三方贡献/依赖来源。若实际 CI 失败，保留记录并修复，不能隐藏失败或把有效 YAML 当 PASS。采用 Debug Artifact 后不再要求创建长期 Release 密钥或 GitHub Release。

首次上传步骤见 FIRST-UPLOAD.md，人工回归见 MANUAL-REGRESSION.md。全程没有设备操作、Root、模拟器、安装、正式私钥创建或自动上传。
