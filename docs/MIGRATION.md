# 首次安装与 Debug APK 签名注意事项

GMBG 首次公开版本为 `1.0.0-rc1` / 40005，包名为 `dev.cheeseowl.gmbg`。

## 首次安装

1. 手动安装 APK，在支持现代 libxposed API102 的 LSPosed 兼容框架中启用模块。
2. 作用域仅选择小米「搜索」`com.android.quicksearchbox`，按框架要求重启。
3. 打开 GMBG，选择 Google 或 Bing，分别设置搜索引擎接管与默认浏览器重定向开关。
4. 避免多个同类模块同时干预小米搜索。

## Debug 签名与后续安装

GitHub Actions 使用正常 Android Debug 签名流程。不同工作流运行可能使用不同证书，不能保证直接覆盖安装。签名不兼容时，可能需要卸载后重新安装，模块配置可能丢失；安装后应重新核对 LSPosed 作用域和开关。

项目不维护专用 Release 签名密钥，不将 Debug 密钥或密码提交到源码仓库。Debug Artifact 不等同于正式稳定发行版。

## 当前验证状态

40005 / `dev.cheeseowl.gmbg` 已由用户手动确认正常工作（**USER_REPORTED**）：Xiaomi 15 Pro / `haotian` / HyperOS `OS3.0.309` / Android 16 / QSB `12.10.0.07203`。该反馈不代表其他设备或所有回归项目均已验证。

GitHub Actions 云端运行仍为 **NOT_RUN**。后续人工检查见 [人工回归清单](MANUAL-REGRESSION.md)，构建与 Artifact 说明见 [CI](CI.md)。
