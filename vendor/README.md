# Compile-only libxposed API 102

`libxposed-api-102.0.0.jar` is the unmodified `classes.jar` extracted from the
official Maven Central AAR `io.github.libxposed:api:102.0.0`.

- AAR: https://repo.maven.apache.org/maven2/io/github/libxposed/api/102.0.0/api-102.0.0.aar
- AAR SHA-256: `423484a6e1807e7a423c4b88fcd8176d104318259d91791877fed88fe91479d0`
- classes.jar SHA-256: `a515dd7a53cd7a47c05e101dff77d61acb3091a97b20a885b9ea3494412db985`
- Publisher listed by the POM: libxposed.
- License: Apache-2.0; complete text in [LICENSES/Apache-2.0.txt](../LICENSES/Apache-2.0.txt).

Only the extracted API Jar is distributed here; the AAR and source repositories
are not build inputs. The Jar is **compileOnly** and is not packaged into the APK.
The runtime framework must provide the modern API102 actually used by this module.
The original Maven POM points at `github.com/libxposed/api`.
The API signatures were also checked against the public
[JingMatrix/libxposed-api](https://github.com/JingMatrix/libxposed-api/tree/39cac0845771547c9c67a3e3ce255af110a54a0e)
snapshot. This is not a claim that every framework branch implements them identically.

No API implementation source was copied or modified. No separate NOTICE file was
found in the retained API source snapshot. Existing notices in distributed artifacts
and the complete Apache license must remain available when redistributing them.
