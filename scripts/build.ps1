param([switch]$Offline, [string]$HostAgent)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputRoot = Join-Path $projectRoot '.local'
New-Item -ItemType Directory -Path $outputRoot -Force | Out-Null
$apiJar = Join-Path $projectRoot 'vendor/libxposed-api-102.0.0.jar'
if ((Get-FileHash -LiteralPath $apiJar -Algorithm SHA256).Hash.ToLowerInvariant() -ne 'a515dd7a53cd7a47c05e101dff77d61acb3091a97b20a885b9ea3494412db985') {
    throw 'Compile-only API102 checksum mismatch'
}
$arguments = @('--no-daemon', '--console=plain', '--project-cache-dir', (Join-Path $outputRoot 'gradle'),
    ':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug')
if ($Offline) { $arguments += '--offline' }
$previousJavaOptions = $env:JAVA_OPTS
if ($HostAgent) {
    $HostAgent = (Resolve-Path -LiteralPath $HostAgent).Path
    $env:JAVA_OPTS = "$previousJavaOptions -javaagent:`"$HostAgent`""
    $arguments += "-Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8 -javaagent:$HostAgent"
    $arguments += "-PgmbgHostAgent=$HostAgent"
}
Push-Location -LiteralPath $projectRoot
try {
    & (Join-Path $projectRoot 'gradlew.bat') @arguments *> (Join-Path $outputRoot 'build.log')
    $buildExit = $LASTEXITCODE
    Get-Content -LiteralPath (Join-Path $outputRoot 'build.log') | Select-Object -Last 35
    if ($buildExit -ne 0) { throw "Gradle failed ($buildExit); see .local/build.log" }
} finally {
    Pop-Location
    $env:JAVA_OPTS = $previousJavaOptions
}
