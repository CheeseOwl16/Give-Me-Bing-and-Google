# 首次上传指南（仅供作者手动执行）

本轮没有初始化 Git、创建远端、提交、push 或发布 Release。以下步骤由作者自行决定和操作，无需向 Codex 提供任何登录凭据。

1. 阅读 LICENSE、NOTICE、ACKNOWLEDGEMENTS 与 PREPUBLICATION-AUDIT；确认拟公开文件及维护/测试状态描述。自有代码采用 Apache-2.0，公开署名 Cheese owl。
2. 在本目录检查 `PUBLIC-FILES.md`、`PUBLIC-FILES.sha256`。只上传这些公开文件，不能把含 `.local/` 的工作目录整体压缩/拖拽上传。
3. 如采用 Git，可自行初始化 main 分支，按公开清单暂存并查看实际 Git 索引/差异；核对没有 `.local/`、build/.gradle、local.properties、APK、私钥或日志。不要把忽略规则当作唯一审查。
4. 在 GitHub 自行创建空仓库 **Give-Me-Bing-and-Google**，避免另行生成与本地冲突的 README/许可证。自行绑定实际仓库地址并上传；本说明不虚构用户名或下载 URL。
5. 检查 Actions 是否启用。main push 应触发 **Android Debug**，也可从 Actions 手动触发。先等公开检查、137 项 JVM 测试、Lint、assembleDebug 和 APK 校验全部通过，再记该次 CI PASS。
6. 打开成功运行，在 Artifacts 下载 `GMBG-Debug-运行号-尝试号` ZIP，核对 APK SHA256。Artifact 有保留期，下载权限受 GitHub 当时的政策与可见性影响。
7. 由用户本人手动测试新身份，先停用旧模块，重新配置 QSB 作用域和开关。跨次 Debug 证书可能不同，卸载重装可能丢配置。按照 MANUAL-REGRESSION 清单记录实际结果。

公开源码不代表候选已实机通过、CI 已成功或项目得到专业安全审计。不创建 GitHub Release、不提交构建 APK、不要求长期 Release 签名密钥，也不承诺维护响应时间。
