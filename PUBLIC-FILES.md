# 拟提交公开文件清单

共 84 个文件，包含此清单和 SHA256 清单。自有源码采用 Apache-2.0，第三方许可单独保留；公开范围以本清单为准。

`.local/` 为本地构建、阶段快照、工具及验证记录，不属于源码提交集合；不得压缩/拖拽整个目录上传。公开集合不含 local.properties、APK、设备日志、研究输入、签名材料或缓存。CI 从源码构建 Debug Artifact，不把产物回写分支。

```text
.gitattributes
.github/workflows/android-debug.yml
.gitignore
ACKNOWLEDGEMENTS.md
LICENSE
LICENSES/Apache-2.0.txt
LICENSES/Gradle-Wrapper.txt
NOTICE
PUBLIC-FILES.md
PUBLIC-FILES.sha256
README.md
README_EN.md
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/dev/cheeseowl/gmbg/Around.java
app/src/main/java/dev/cheeseowl/gmbg/BrowserDispatcher.java
app/src/main/java/dev/cheeseowl/gmbg/BrowserEngine.java
app/src/main/java/dev/cheeseowl/gmbg/BrowserRedirect.java
app/src/main/java/dev/cheeseowl/gmbg/CatalogPolicy.java
app/src/main/java/dev/cheeseowl/gmbg/DecisionLease.java
app/src/main/java/dev/cheeseowl/gmbg/DiagnosticLog.java
app/src/main/java/dev/cheeseowl/gmbg/DiagnosticModule.java
app/src/main/java/dev/cheeseowl/gmbg/DispatchDecision.java
app/src/main/java/dev/cheeseowl/gmbg/EngineChoice.java
app/src/main/java/dev/cheeseowl/gmbg/EngineHost.java
app/src/main/java/dev/cheeseowl/gmbg/EngineTakeover.java
app/src/main/java/dev/cheeseowl/gmbg/FeatureClient.java
app/src/main/java/dev/cheeseowl/gmbg/FeatureProvider.java
app/src/main/java/dev/cheeseowl/gmbg/FeatureState.java
app/src/main/java/dev/cheeseowl/gmbg/HistoryDomain.java
app/src/main/java/dev/cheeseowl/gmbg/HistoryHost.java
app/src/main/java/dev/cheeseowl/gmbg/HistoryScope.java
app/src/main/java/dev/cheeseowl/gmbg/InfoActivity.java
app/src/main/java/dev/cheeseowl/gmbg/LauncherIconController.java
app/src/main/java/dev/cheeseowl/gmbg/NativeSuggestionDomain.java
app/src/main/java/dev/cheeseowl/gmbg/NativeSuggestionHost.java
app/src/main/java/dev/cheeseowl/gmbg/NativeSuggestionScope.java
app/src/main/java/dev/cheeseowl/gmbg/Privacy.java
app/src/main/java/dev/cheeseowl/gmbg/QueryScope.java
app/src/main/java/dev/cheeseowl/gmbg/QueryStatus.java
app/src/main/java/dev/cheeseowl/gmbg/ReadState.java
app/src/main/java/dev/cheeseowl/gmbg/SearchUrlPolicy.java
app/src/main/java/dev/cheeseowl/gmbg/SettingsPolicy.java
app/src/main/java/dev/cheeseowl/gmbg/SuggestionDiagnostics.java
app/src/main/java/dev/cheeseowl/gmbg/SuggestionKinds.java
app/src/main/java/dev/cheeseowl/gmbg/TakeoverCapability.java
app/src/main/java/dev/cheeseowl/gmbg/TargetSpec.java
app/src/main/java/dev/cheeseowl/gmbg/Trace.java
app/src/main/java/dev/cheeseowl/gmbg/WebPolicy.java
app/src/main/res/drawable/ic_diagnostic.xml
app/src/main/res/values/ids.xml
app/src/main/res/values/strings.xml
app/src/main/res/xml/data_extraction_rules.xml
app/src/main/resources/META-INF/xposed/java_init.list
app/src/main/resources/META-INF/xposed/module.prop
app/src/main/resources/META-INF/xposed/scope.list
app/src/test/java/dev/cheeseowl/gmbg/BrowserTest.java
app/src/test/java/dev/cheeseowl/gmbg/DiagnosticCoreTest.java
app/src/test/java/dev/cheeseowl/gmbg/HistoryTest.java
app/src/test/java/dev/cheeseowl/gmbg/LauncherIconTest.java
app/src/test/java/dev/cheeseowl/gmbg/NativeSuggestionTest.java
app/src/test/java/dev/cheeseowl/gmbg/SuggestionDiagnosticsTest.java
app/src/test/java/dev/cheeseowl/gmbg/TakeoverTest.java
build.gradle.kts
docs/BUILDING.md
docs/CI.md
docs/FIRST-UPLOAD.md
docs/MANUAL-REGRESSION.md
docs/MIGRATION.md
docs/PREPUBLICATION-AUDIT.md
gradle.properties
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
gradlew
gradlew.bat
scripts/build.ps1
scripts/dex_inspect.py
scripts/package_debug.py
scripts/preflight.py
scripts/prepare_ci_sdk.py
scripts/verify.py
settings.gradle.kts
vendor/README.md
vendor/libxposed-api-102.0.0.jar
```

PUBLIC-FILES.sha256 覆盖本清单内除 SHA 文件自身的所有内容。两份 Jar 是已核验许可的 Wrapper/API 编译输入。修改文件后需重新核对并更新清单；preflight 将拒绝未匹配文件或摘要。

## 跨平台摘要规则

`.gitattributes` 规定文本以 LF 存入 Git 并检出；Jar 等二进制禁止换行转换。SHA256 清单覆盖 83 项，不包含清单自身。

修改公开文件后，先核对并暂存确切的公开集合，再执行：

```sh
python scripts/preflight.py --write-manifest
python scripts/preflight.py
```

生成器对实际 Git 索引 Blob 求 SHA-256，并要求它与工作区的 LF 文本内容一致；发现未暂存编辑会停止。最后暂存更新后的 `PUBLIC-FILES.sha256`。不要将未审查文件加入索引。

普通校验仅将文本 CRLF 规范为 LF 后计算摘要，二进制保持原字节；在 Ubuntu 的 LF 检出中，摘要即实际文件字节摘要。正文、空格、BOM 或二进制的其他变化仍会使校验失败。不能通过重新生成清单代替审阅改动。
