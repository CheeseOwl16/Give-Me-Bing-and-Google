# GMBG 人工回归

此清单仅供用户手动操作。当前版本为 1.0.0-rc1。40005 / `dev.cheeseowl.gmbg` 已由用户手动确认正常工作（**USER_REPORTED**）：Xiaomi 15 Pro / `haotian` / HyperOS `OS3.0.309` / Android 16 / QSB `12.10.0.07203`。该反馈不代表其他设备或所有回归项目均已验证。以下条目用于后续回归，不表示全部已执行。

1. 在 Hook 框架中检查搜索作用域，避免多个同类模块同时干预 QSB；按框架要求重启。
2. 手动安装 GMBG，检查桌面及框架显示 Give Me Bing and Google!，模块被识别为现代 API102 模块。
3. 给模块设置仅「搜索」作用域，打开设置核对初始开关、Bing/Google 单选。
4. 两开关开：分别选 Google/Bing，测试底部搜索、普通网页建议、键盘历史、完整历史搜索词；检查实际浏览器/网页引擎及固定非敏感查询语义。
5. 回归完整历史网址直达、首页历史回填/网络跳转、本地应用、相册图片、短信、笔记、通讯录、系统设置及现有特殊服务。没有项目记 NOT_RUN，不制造私有数据库条目。
6. 测试两开关四种组合：全部原生；仅引擎；仅默认浏览器；二者均开。关闭引擎后调整原生选项，确认原生行为恢复。
7. 在适用普通网页场景检查百度/360/抖音等原生选项不会改变模块指定 Google/Bing；不把专有协议/特殊服务误判为普通网页。
8. 保留默认 Chrome 及一次其他系统默认浏览器的用户可选检查；设置小米浏览器为默认时也应尊重选择，未经测试的项目不写 PASS。
9. 手动重启目标进程/设备，检查模块配置保存及生效；检查日志无重复 Hook、重复打开或 Provider 配置不可读。

日志沿用 QSBP2 schema，使用 session/call/parent 关联。GATE_OK、TAKEOVER_READY、NATIVE_READY、HISTORY_READY、ITEM_READY、TEMPLATE_OK、ENCODING_OK、URL_CHECK MATCH/FINAL_OK 与 BROWSER_ROUTE 只能分别证明各阶段。START_REQUESTED 不能替代实际网页观察。

反馈只给入口类别、开关、引擎枚举、预期/实际动作和脱敏事件。不分享原词、完整 URL、搜索历史、模板或 extras。异常时停用模块并按框架要求恢复。不要清空目标数据以凑通过。
