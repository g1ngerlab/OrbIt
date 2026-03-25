param(
    [switch]$NoDaemon,
    [switch]$PlainConsole
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Path $PSScriptRoot -Parent
$wrapperJar = Join-Path $repoRoot "gradle\wrapper\gradle-wrapper.jar"

if (-not (Test-Path $wrapperJar)) {
    throw "Gradle wrapper jar not found: $wrapperJar"
}

$arguments = @("-classpath", $wrapperJar, "org.gradle.wrapper.GradleWrapperMain")
if ($NoDaemon) { $arguments += "--no-daemon" }
if ($PlainConsole) { $arguments += "--console"; $arguments += "plain" }
$arguments += "Server:run"

Push-Location $repoRoot
try {
    & java @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Server run failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}

