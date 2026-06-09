param(
    [string]$FantasyPetRuleRoot = "",
    [string]$AppJobId = "public-lifecycle-smoke",
    [string]$Description = "A tiny stardust dragon desktop pet with smooth idle motion.",
    [ValidateSet("balanced", "wide", "wide-tail", "tall")]
    [string]$BodyShape = "wide-tail",
    [switch]$KeepRunRoot
)

$ErrorActionPreference = "Stop"

function Assert-SmokeCondition {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Get-SmokeFreePort {
    $listener = [System.Net.Sockets.TcpListener]::new(
        [System.Net.IPAddress]::Parse("127.0.0.1"),
        0
    )
    try {
        $listener.Start()
        return $listener.LocalEndpoint.Port
    } finally {
        $listener.Stop()
    }
}

function Remove-SmokeRunRoot {
    param([string]$RunRoot)

    $resolvedTemp = (Resolve-Path -LiteralPath ([System.IO.Path]::GetTempPath())).Path
    $resolvedRunRoot = (Resolve-Path -LiteralPath $RunRoot -ErrorAction SilentlyContinue).Path
    if ($resolvedRunRoot -and $resolvedRunRoot.StartsWith($resolvedTemp)) {
        Remove-Item -LiteralPath $resolvedRunRoot -Recurse -Force
    }
}

function Read-SmokeZipEntryText {
    param(
        [string]$ZipPath,
        [string]$EntryName
    )

    Add-Type -AssemblyName System.IO.Compression
    $zipStream = [System.IO.File]::OpenRead($ZipPath)
    $archive = $null
    try {
        $archive = [System.IO.Compression.ZipArchive]::new(
            $zipStream,
            [System.IO.Compression.ZipArchiveMode]::Read
        )
        $entry = $archive.GetEntry($EntryName)
        Assert-SmokeCondition ($null -ne $entry) "ZIP entry missing: $EntryName"
        Assert-SmokeCondition ($entry.Length -gt 0) "ZIP entry empty: $EntryName"

        $entryStream = $entry.Open()
        $reader = [System.IO.StreamReader]::new($entryStream, [System.Text.Encoding]::UTF8)
        try {
            return $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
            $entryStream.Dispose()
        }
    } finally {
        if ($archive) {
            $archive.Dispose()
        }
        $zipStream.Dispose()
    }
}

function Start-SmokeCommunityApi {
    param(
        [string]$GamerRoot,
        [int]$Port
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = "node"
    $startInfo.Arguments = "services/community-api/src/server.js"
    $startInfo.WorkingDirectory = $GamerRoot
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.EnvironmentVariables["PORT"] = [string]$Port
    return [System.Diagnostics.Process]::Start($startInfo)
}

$gamerRoot = Split-Path -Parent $PSScriptRoot
$safeRunId = ($AppJobId -replace "[^A-Za-z0-9._-]+", "-").Trim("-")
if ([string]::IsNullOrWhiteSpace($safeRunId)) {
    $safeRunId = "public-lifecycle-smoke"
}

$communityProcess = $null
$lifecycle = $null

try {
    $lifecycleArgs = @{
        AppJobId = $AppJobId
        Description = $Description
        BodyShape = $BodyShape
        KeepRunRoot = $true
    }
    if (-not [string]::IsNullOrWhiteSpace($FantasyPetRuleRoot)) {
        $lifecycleArgs.FantasyPetRuleRoot = $FantasyPetRuleRoot
    }

    $lifecycleOutput = & (Join-Path $PSScriptRoot "smoke-fantasy-pet-public-lifecycle.ps1") @lifecycleArgs
    $lifecycle = ($lifecycleOutput -join "`n") | ConvertFrom-Json
    Assert-SmokeCondition ($lifecycle.status -eq "passed") "public lifecycle smoke did not pass"
    Assert-SmokeCondition (-not [string]::IsNullOrWhiteSpace([string]$lifecycle.runRoot)) "public lifecycle smoke did not keep runRoot"

    $packagePath = Join-Path ([string]$lifecycle.runRoot) (Join-Path $safeRunId "package\pet.zip")
    Assert-SmokeCondition (Test-Path -LiteralPath $packagePath) "pet.zip not found: $packagePath"
    $packageFile = Get-Item -LiteralPath $packagePath
    $manifestText = Read-SmokeZipEntryText -ZipPath $packagePath -EntryName "package-manifest.json"
    $packageManifest = $manifestText | ConvertFrom-Json
    Assert-SmokeCondition ($packageManifest.schema -eq "fantasy-pet.package-manifest.v1") "unexpected package manifest schema"
    Assert-SmokeCondition ($packageManifest.acceptedBy -eq "human-review") "package manifest was not human reviewed"

    $port = Get-SmokeFreePort
    $communityBaseUrl = "http://127.0.0.1:$port"
    $communityProcess = Start-SmokeCommunityApi -GamerRoot $gamerRoot -Port $port

    $health = $null
    $lastError = $null
    for ($attempt = 0; $attempt -lt 80; $attempt++) {
        Assert-SmokeCondition (-not $communityProcess.HasExited) "community API exited before readiness"
        try {
            $health = Invoke-RestMethod -Method Get -Uri ($communityBaseUrl + "/health") -TimeoutSec 3
            break
        } catch {
            $lastError = $_.Exception.Message
            Start-Sleep -Milliseconds 250
        }
    }
    Assert-SmokeCondition ($null -ne $health) "community API was not ready: $lastError"
    Assert-SmokeCondition ($health.service -eq "community-api") "unexpected community API health response"

    $importBody = @{
        packageManifest = $packageManifest
        packageFileName = $packageFile.Name
        packageByteCount = [int64]$packageFile.Length
        targetDownloadId = [string]$lifecycle.candidateDownloadId
        ownershipClaimId = "claim-$safeRunId"
    } | ConvertTo-Json -Depth 30

    $draft = Invoke-RestMethod `
        -Method Post `
        -Uri ($communityBaseUrl + "/v1/import-drafts/from-fantasy-pet-package") `
        -ContentType "application/json" `
        -Body $importBody `
        -TimeoutSec 10
    Assert-SmokeCondition ($draft.readiness.status -eq "community-ready") "community import draft was not ready"
    Assert-SmokeCondition ($draft.importSummary.review.targetDownloadId -eq [string]$lifecycle.candidateDownloadId) "community import targetDownloadId mismatch"
    Assert-SmokeCondition ($draft.importSummary.source.appJobId -eq $AppJobId) "community import appJobId mismatch"

    $submitBody = @{
        draftId = [string]$draft.id
    } | ConvertTo-Json -Depth 5
    $submitResult = Invoke-RestMethod `
        -Method Post `
        -Uri ($communityBaseUrl + "/v1/import-drafts/submit") `
        -ContentType "application/json" `
        -Body $submitBody `
        -TimeoutSec 10
    Assert-SmokeCondition ($submitResult.submission.status -eq "pending") "community submission was not pending"
    Assert-SmokeCondition ($submitResult.submission.importDraftId -eq $draft.id) "community submission draft mismatch"
    Assert-SmokeCondition ($submitResult.draft.status -eq "submitted") "community import draft was not submitted"

    [pscustomobject]@{
        schema = "gamer.fantasy-pet-community-import-smoke.v1"
        status = "passed"
        appJobId = $AppJobId
        lifecycleStatus = $lifecycle.status
        candidateDownloadId = $lifecycle.candidateDownloadId
        packageFileName = $packageFile.Name
        packageByteCount = [int64]$packageFile.Length
        importDraftId = $draft.id
        importDraftReadiness = $draft.readiness.status
        importTargetDownloadId = $draft.importSummary.review.targetDownloadId
        submissionId = $submitResult.submission.id
        submissionStatus = $submitResult.submission.status
        submittedDraftStatus = $submitResult.draft.status
        runRoot = if ($KeepRunRoot) { [string]$lifecycle.runRoot } else { "" }
    } | ConvertTo-Json -Depth 8
} finally {
    if ($communityProcess -and -not $communityProcess.HasExited) {
        Stop-Process -Id $communityProcess.Id -Force -ErrorAction SilentlyContinue
    }
    if ($lifecycle -and -not $KeepRunRoot) {
        Remove-SmokeRunRoot -RunRoot ([string]$lifecycle.runRoot)
    }
}
