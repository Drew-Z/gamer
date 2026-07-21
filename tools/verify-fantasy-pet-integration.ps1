param(
    [string]$GradleWrapper = "D:\workspace4Cursor\pet\floating-pet-android\gradlew.bat",
    [switch]$IncludeAndroidUi,
    [int]$AndroidUiPublicApiPort = 18765
)

$ErrorActionPreference = "Stop"

function Invoke-ExternalStep {
    param(
        [string]$Name,
        [string]$FilePath,
        [string[]]$ArgumentList,
        [string]$WorkingDirectory
    )

    Write-Host ""
    Write-Host "== $Name"
    Push-Location $WorkingDirectory
    try {
        & $FilePath @ArgumentList
        $exitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }

    if ($exitCode -ne 0) {
        throw "$Name failed with exit code $exitCode"
    }
}

function Invoke-ForbiddenAndroidMainScan {
    param([string]$AndroidMainRoot)

    Write-Host ""
    Write-Host "== Android public-app forbidden surface scan: rg -n -e /admin"
    $scanArgs = @(
        "-n",
        "-e",
        "/admin",
        "-e",
        "server-worker-cycle",
        "-e",
        "agent-outputs",
        "-e",
        "Codex",
        "-e",
        "GenericAgent",
        "-e",
        "targetOutput",
        "-e",
        "file://",
        "-e",
        "lease",
        "-e",
        "prompt-pack",
        "-e",
        "adapter-config",
        "-e",
        "SSH",
        $AndroidMainRoot
    )
    & rg @scanArgs
    $exitCode = $LASTEXITCODE
    if ($exitCode -eq 0) {
        throw "Android main source contains forbidden public-app surface terms"
    }
    if ($exitCode -ne 1) {
        throw "Forbidden surface scan failed with exit code $exitCode"
    }
}

$gamerRoot = Split-Path -Parent $PSScriptRoot
$androidRoot = Join-Path $gamerRoot "apps\android-community"
$androidMainRoot = Join-Path $androidRoot "app\src\main"

Invoke-ExternalStep `
    -Name "npm.cmd test" `
    -FilePath "npm.cmd" `
    -ArgumentList @("test") `
    -WorkingDirectory $gamerRoot

Invoke-ExternalStep `
    -Name "Android testDebugUnitTest --console=plain" `
    -FilePath $GradleWrapper `
    -ArgumentList @("-p", $androidRoot, "testDebugUnitTest", "--console=plain") `
    -WorkingDirectory $gamerRoot

Invoke-ExternalStep `
    -Name "Android assembleDebug --console=plain" `
    -FilePath $GradleWrapper `
    -ArgumentList @("-p", $androidRoot, "assembleDebug", "--console=plain") `
    -WorkingDirectory $gamerRoot

if ($IncludeAndroidUi) {
    Invoke-ExternalStep `
        -Name "Android connectedDebugAndroidTest --console=plain" `
        -FilePath $GradleWrapper `
        -ArgumentList @("-p", $androidRoot, "connectedDebugAndroidTest", "--console=plain") `
        -WorkingDirectory $gamerRoot

    Invoke-ExternalStep `
        -Name "Android contract demo UI smoke" `
        -FilePath (Join-Path $PSScriptRoot "launch-fantasy-pet-android-ui-smoke.cmd") `
        -ArgumentList @(
            "-StartPublicApi",
            "-AssertContractDemoUi",
            "-StopPublicApiOnExit",
            "-PublicApiPort",
            [string]$AndroidUiPublicApiPort,
            "-FantasyPetApiBaseUrl",
            "http://10.0.2.2:$AndroidUiPublicApiPort"
        ) `
        -WorkingDirectory $gamerRoot
}

Invoke-ExternalStep `
    -Name "Fantasy pet public lifecycle smoke" `
    -FilePath (Join-Path $PSScriptRoot "smoke-fantasy-pet-public-lifecycle.cmd") `
    -ArgumentList @() `
    -WorkingDirectory $gamerRoot

Invoke-ExternalStep `
    -Name "Fantasy pet community import smoke" `
    -FilePath (Join-Path $PSScriptRoot "smoke-fantasy-pet-community-import.cmd") `
    -ArgumentList @() `
    -WorkingDirectory $gamerRoot

Invoke-ForbiddenAndroidMainScan -AndroidMainRoot $androidMainRoot

Invoke-ExternalStep `
    -Name "git diff --check" `
    -FilePath "git" `
    -ArgumentList @("diff", "--check") `
    -WorkingDirectory $gamerRoot

$checks = @(
    "npm.cmd test",
    "testDebugUnitTest --console=plain",
    "assembleDebug --console=plain"
)
if ($IncludeAndroidUi) {
    $checks += "connectedDebugAndroidTest --console=plain"
    $checks += "launch-fantasy-pet-android-ui-smoke.cmd -StartPublicApi -AssertContractDemoUi"
}
$checks += @(
    "smoke-fantasy-pet-public-lifecycle.cmd",
    "smoke-fantasy-pet-community-import.cmd",
    "rg -n -e /admin",
    "git diff --check"
)

[pscustomobject]@{
    schema = "gamer.fantasy-pet-integration-verification.v1"
    status = "passed"
    checks = $checks
} | ConvertTo-Json -Depth 4
