# 第三方来源、致谢及授权状态

项目自有代码由 Cheese owl 采用 Apache-2.0 许可，见根 LICENSE 和 NOTICE。仅覆盖作者有权许可的材料；第三方材料不会因此被重新授权。此文件不授予他人代码的额外权利。**致谢不等于获得版权许可。**

## 参考项目：没有随候选分发其实现源码

| 项目/作者 | 固定审查版本 | 许可证与参考内容 | 本候选关系 |
|---|---|---|---|
| Hyper Helper / XiaomiHelper，HowieHChen 及贡献者；包命名使用 lackluster | `649d39928ae173d36db77ce93a57f32662044e34` | [仓库](https://github.com/HowieHChen/XiaomiHelper)，[LICENSE](https://github.com/HowieHChen/XiaomiHelper/blob/649d39928ae173d36db77ce93a57f32662044e34/LICENSE)：GNU GPL Version 3。具体搜索文件没有逐文件 or-later 授权；根文本的示例条款不能自动确认为全项目 or-later。参考搜索引擎 item 与 scene 扩展思路 | 没有导入/翻译该项目 Hook、框架、模型实现、图标或自定义引擎 UI；本候选为限定请求 item/模板接管，不修改其列表/scene。未发现修改后的上游实现文件 |
| Fxxk-MiBrowser，DuhMatt | `685a86a5990cd0e06418732e70b26867055c03c4` | [仓库](https://github.com/DuhMatt/Fxxk-MiBrowser)，[LICENSE](https://github.com/DuhMatt/Fxxk-MiBrowser/blob/685a86a5990cd0e06418732e70b26867055c03c4/LICENSE)：MIT，Copyright (c) 2026 DuhMatt。参考默认浏览器、HTTP/HTTPS 范围及重入处理思路 | 没有导入/改写其 Kotlin 实现；其全局 Context/Instrumentation 方案不是本候选的 QSB 特定业务方法方案。未使用其资源或 URL 缓存实现 |

具体审查的 XiaomiHelper 文件：`MoreSearchEngine.kt`、`Search.kt`、`SearchEngineItem.kt`、`Preferences.kt`、`HookEntry.kt`、根 LICENSE/README；均相对于上游 `app/src/main/kotlin/dev/lackluster/mihelper/`（根文件除外）。搜索规则位于 `hook/rules/search/`，模型位于 `data/model/`。

完整的源码相对位置：`hook/rules/search/MoreSearchEngine.kt`、`hook/scopes/Search.kt`、`data/model/SearchEngineItem.kt`、`data/preference/Preferences.kt`、`hook/HookEntry.kt`。

具体审查的 Fxxk-MiBrowser 文件：`app/src/main/java/com/hyperosfix/browser/MainHook.kt`、`IntentInterceptor.kt`、`DefaultBrowserResolver.kt`、根 LICENSE/README。

上述固定 LICENSE 均与本地保留版本核对。2026-10-20 匿名网页复核可读取 XiaomiHelper 固定 LICENSE；Fxxk 固定页面读取失败，因此其结论以已有固定源码中的完整 MIT 文本为准，未请求登录或使用账户。没有把最新主分支徽标当作固定版本许可。

审查方法：核对候选适配类与上述实现的结构、API 和来源记录；对 XiaomiHelper 345 个、Fxxk 11 个 Java/Kotlin 文件与本模块主源码比较，未找到连续 80-token 完全相同片段。这不排除短片段、翻译或结构性相似，也不是法律上的独立创作证明。具体兼容签名、字段、引擎 ID 和模板接口事实不等于复制整个专有/开源实现。

## 实际分发材料

| 材料 | 授权/处理 | 是否进入 APK |
|---|---|---|
| `vendor/libxposed-api-102.0.0.jar` | libxposed 发布，Apache-2.0；从官方 Maven AAR 提取的未修改 classes.jar；保留完整 [Apache-2.0](LICENSES/Apache-2.0.txt)。获取方式、摘要见 [vendor](vendor/README.md) | 否，compileOnly |
| `gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.jar` | Gradle Wrapper，Apache-2.0。脚本原版权头保留；Jar 内 LICENSE 保留，并另存 [完整文本](LICENSES/Gradle-Wrapper.txt)。未改写 Wrapper 实现，properties 固定版本与下载校验 | 否 |

API 定义还核对了公开 [JingMatrix/libxposed-api](https://github.com/JingMatrix/libxposed-api/tree/39cac0845771547c9c67a3e3ce255af110a54a0e) 和 [Vector](https://github.com/JingMatrix/Vector/tree/ddeed8ca1ffedfaba7f4341b911089906862d5c4)；没有将其框架或管理器实现加入候选。它们不是对用户框架全部实现的证明。API 源码快照及分发 Jar 未发现单独 NOTICE 文件；若今后更换依赖，需重新检查。

## 仅供构建/测试的依赖

- JUnit 4.13.2：EPL-1.0；Hamcrest Core 1.3：BSD-3-Clause。由 Gradle 解析，仅 JVM 测试，不随源码候选附带 Jar，不进入 APK。
- Gradle 9.5.0、AGP 9.3.3、JDK21、Android SDK36.1 / Build Tools36.0.0：外部工具链，不将其发行包或源码搬入本仓库。
- Debug APK 含 AGP 根据 Android boot classpath 自动生成的 global synthetics/SDK API model 支持定义，不应宣传成“只有自有包类”。本地检查将该 DEX 与构建工具输出逐字节比较；未复制 QSB 二进制。正式 APK 发行时仍需审阅工具生成代码与 SDK/R8 适用通知，源码候选审核不能替代最终发行闭包审查。
- 本机可选 JVM 兼容 agent 不分发，也不是应用依赖；公开构建脚本只接受使用者显式提供的可信文件路径。

## 已采用的项目许可及保留边界

作者已明确选择 **Apache License, Version 2.0（Apache-2.0）**，公开署名 **Cheese owl**。根 LICENSE 使用 Apache 官方完整文本；根 NOTICE 仅记录项目署名和许可范围，不是额外使用限制。LICENSES/ 中既有第三方文本、Wrapper 版权头和二进制内通知保持原样。

先前的许可证选项讨论已由此决定取代；不再将自有代码标为待选或暂定 GPL。项目级声明覆盖自有 Java/测试、资源、构建与检查脚本、文档；不为添加头部而改动冻结的 Java，也不修改第三方文件的版权头。新增自有工作流/脚本使用 SPDX-License-Identifier: Apache-2.0。

Apache-2.0 的采用不是已证明完全无版权风险。此前未发现 Hyper Helper、Fxxk 实现代码被直接复用的结论及其局限仍保留。若发现 GPL 实现翻译/衍生或未知授权材料，应停止相关发行，重新确认准确授权、整体兼容性、修改通知和对应源码义务，不能用致谢或本项目 Apache 声明覆盖。MIT 重要部分若被复用，仍需保留 DuhMatt 版权和完整许可。

本项目仅通过 GitHub Actions 生成 Debug APK Artifact，不维护专用 Release keystore、不创建 GitHub Release。Artifact 随附本项目 LICENSE/NOTICE、致谢及已保留的许可文本；不上传签名材料或工具缓存。生成支持定义/SDK/R8 的来源说明保持，本次来源复核不称作专业安全审计。

## CI 工具

工作流使用 GitHub 官方 actions/checkout、actions/setup-java、actions/upload-artifact，固定到已核验的完整 Commit；版本和链接见 docs/CI.md。它们在 GitHub 托管环境执行，没有将其实现源码导入项目，没有私人 PAT，也没有新增模块运行依赖。

参考 [Apache 官方许可](https://www.apache.org/licenses/LICENSE-2.0)。自有许可已落实，但所有新框架/依赖/第三方贡献仍需按实际来源重新检查。
