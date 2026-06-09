param(
    [string]$FantasyPetRuleRoot = "",
    [string]$AppJobId = "public-lifecycle-smoke",
    [string]$Description = "A tiny stardust dragon desktop pet with smooth idle motion.",
    [ValidateSet("balanced", "wide", "wide-tail", "tall")]
    [string]$BodyShape = "wide-tail",
    [switch]$KeepRunRoot
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "fantasy-pet-smoke-image.ps1")

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

function Stop-SmokeServerProcesses {
    param([string]$RunRoot)

    $serverProcesses = @(
        Get-CimInstance Win32_Process -Filter "name = 'python.exe'" |
            Where-Object { $_.CommandLine -and $_.CommandLine.Contains($RunRoot) }
    )
    foreach ($serverProcess in $serverProcesses) {
        Stop-Process -Id $serverProcess.ProcessId -Force -ErrorAction SilentlyContinue
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
        [byte[]]$ZipBytes,
        [string]$EntryName
    )

    Add-Type -AssemblyName System.IO.Compression
    $zipStream = [System.IO.MemoryStream]::new($ZipBytes, $false)
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

function Get-SmokeInternalMarkers {
    return @(
        "server_run.json",
        "artifact-index.json",
        "resolution-map",
        "desktop-pet-casebook-audit.json",
        "desktop-pet-stage-gate-report.json",
        "desktop-pet-learning-memory.json",
        "server-generation-learning-drill.json",
        "human-feedback-context.json",
        "stage-gate-ledger-import.json",
        "learning-ledger.jsonl",
        "route-policy-decision.json",
        "genericagent-ledger-suggestions.json",
        "genericagent-ledger-import.json",
        "genericagent-orchestrator-task.json",
        "codex-worker-task.json",
        "codex-worker-task.output.json",
        "*.invocation.json",
        ".invocation.json",
        "*.execution.json",
        ".execution.json",
        "*.output.json.adapterprovenance",
        ".output.json.adapterprovenance",
        "adapterprovenance",
        "directcodexcli",
        "strategy-plan.json",
        "codex-generation-directives.json",
        "server-proof-summary.json",
        "server-proof-summary",
        "realadapterlaunch",
        "humanacceptance",
        "agent-review.json",
        "orchestration-review.json",
        "learning-drill",
        "learningprogress",
        "learningmemory",
        "learningmemoryresponse",
        "codexgenerationdirectiveresponse",
        "codexgenerationdirectiveresponsepresentcount",
        "codexgenerationdirectiveresponsesummary",
        "codexqaevidence",
        "directivehistoryresponse",
        "narrowedrepairfocus",
        "gadirectivehistoryresponse",
        "gadirectivehistoryresponsepresentcount",
        "gadirectivehistoryaddressedgenerationdirectivetext",
        "gadirectivehistorynarrowedrepairfocus",
        "gadirectivehistorynarrowedrepairfocuscounts",
        "directivehistorynarrowedrepairfocuscountdeltas",
        "repeateddirectivehistorynarrowedrepairfocus",
        "casebookreferencesused",
        "repairstrategies",
        "repairstrategiesused",
        "desktoppetlearningmemorysummary",
        "servergenerationlearningprogresssummary",
        "qualitygatestatus",
        "qualitygatestatuscounts",
        "qualitygatetrend",
        "learningassessment",
        "nextrepairfocus",
        "memorycarryforward",
        "learningmemoryinput",
        "learningmemoryoutput",
        "priormemorypresent",
        "priormemoryqualitygatestatus",
        "priormemoryscenariocount",
        "repeatedneedsrevisionstages",
        "repeatedhardfailuresobserved",
        "missingneedsrevisioncoverage",
        "missinghardfailurecoverage",
        "repaircoverage",
        "repairstrategyusecounts",
        "codex-action-attempt-n-server-imagegen-001",
        "stagegatereport",
        "stagegaterepair",
        "stagegaterepairrequests",
        "stagegatestatus",
        "learningledgersuggestions",
        "routeswitchrequired",
        "disabledroutes",
        "caseid",
        "referencetype",
        "strengthstopreserve",
        "reviewlessons",
        "runs/",
        "secret/",
        "targetoutput",
        "prompt-pack",
        "adapter-config"
    )
}

function Test-SmokeSafePackageRelativePath {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return $false
    }

    $lower = $Path.ToLowerInvariant()
    $segments = @($Path -split "/")
    $internalMarkers = @(Get-SmokeInternalMarkers)

    return -not $lower.StartsWith("file:") `
        -and -not ($Path -match "^[A-Za-z]:[\\/]") `
        -and -not $Path.StartsWith("/") `
        -and -not $Path.Contains('\') `
        -and -not $Path.Contains(":") `
        -and -not ($segments -contains "..") `
        -and -not @($internalMarkers | Where-Object { $lower.Contains($_) }).Count
}

function Assert-SmokeAppApiContract {
    param([object]$Contract)

    Assert-SmokeCondition ($null -ne $Contract) "app API contract missing"
    Assert-SmokeCondition ($Contract.schema -eq "fantasy-pet.app-api-contract.v1") "unexpected contract schema"

    $requiredPublicPaths = @(
        "/app-api-contract",
        "/pet-generation-jobs",
        "/pet-generation-jobs/{appJobId}",
        "/pet-generation-jobs/{appJobId}/artifacts",
        "/pet-generation-jobs/{appJobId}/artifacts/{downloadId}",
        "/pet-generation-jobs/{appJobId}/review-decisions",
        "/pet-generation-jobs/{appJobId}/package",
        "/worker-readiness"
    )
    $publicEndpoints = @(
        $Contract.publicEndpoints | Where-Object { $_.public -ne $false }
    )
    $publicPaths = @($publicEndpoints | ForEach-Object { [string]$_.path })
    foreach ($requiredPublicPath in $requiredPublicPaths) {
        Assert-SmokeCondition ($publicPaths -contains $requiredPublicPath) "contract public endpoint missing: $requiredPublicPath"
    }

    $publicAdminEndpoints = @(
        $publicEndpoints | Where-Object {
            (([string]$_.path).Trim().Split("/") | Select-Object -Index 1 -ErrorAction SilentlyContinue) -eq "admin"
        }
    )
    Assert-SmokeCondition ($publicAdminEndpoints.Count -eq 0) "contract exposes admin endpoint as public"

    $security = $Contract.security
    Assert-SmokeCondition ($null -ne $security) "contract security boundary missing"
    Assert-SmokeCondition ($security.exposesInternalPaths -eq $false) "contract exposesInternalPaths was not false"
    Assert-SmokeCondition ($security.exposesWorkerCommands -eq $false) "contract exposesWorkerCommands was not false"
    Assert-SmokeCondition ($security.exposesSecrets -eq $false) "contract exposesSecrets was not false"
    Assert-SmokeCondition ($security.appMayInvokeAgentsDirectly -eq $false) "contract appMayInvokeAgentsDirectly was not false"
    Assert-SmokeCondition ($security.requiresHumanReview -eq $true) "contract requiresHumanReview was not true"
    Assert-SmokeCondition ($security.adminEndpointsDisabledByDefault -eq $true) "contract adminEndpointsDisabledByDefault was not true"
}

function Assert-SmokeWorkerReadiness {
    param([object]$Readiness)

    Assert-SmokeCondition ($null -ne $Readiness) "worker readiness missing"
    Assert-SmokeCondition ($Readiness.schema -eq "fantasy-pet.worker-readiness-public.v1") "unexpected worker readiness schema"

    $security = $Readiness.security
    Assert-SmokeCondition ($null -ne $security) "worker readiness security boundary missing"
    Assert-SmokeCondition ($security.secretsInReport -eq $false) "worker readiness secretsInReport was not false"
    Assert-SmokeCondition ($security.executesAgentProcesses -eq $false) "worker readiness executesAgentProcesses was not false"
    Assert-SmokeCondition ($security.appMayInvokeAgentsDirectly -eq $false) "worker readiness appMayInvokeAgentsDirectly was not false"
    Assert-SmokeCondition ($security.executesReadinessProbe -eq $false) "worker readiness executesReadinessProbe was not false"
}

function Invoke-SmokeLifecycleDemo {
    param(
        [string]$FantasyPetRuleRoot,
        [string]$RunRoot,
        [string]$AppJobId,
        [string]$RunId,
        [string]$Description,
        [string]$BodyShape
    )

    $runDir = Join-Path $RunRoot $RunId
    $demoArgs = @(
        "run",
        "--with-requirements",
        "requirements-server.txt",
        "python",
        "tools\run_server_job_lifecycle_demo.py",
        "--run-dir",
        $runDir,
        "--app-job-id",
        $AppJobId,
        "--run-id",
        $RunId,
        "--description",
        $Description,
        "--body-shape",
        $BodyShape
    )
    Push-Location $FantasyPetRuleRoot
    try {
        $demoOutput = & uv @demoArgs
        $demoExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    Assert-SmokeCondition ($demoExitCode -eq 0) ("demo failed: " + ($demoOutput -join "`n"))

    $demo = ($demoOutput -join "`n") | ConvertFrom-Json
    Assert-SmokeCondition ($demo.status -eq "needs-human-review") "demo did not stop at human review"
    Assert-SmokeCondition ([int]$demo.artifactCount -gt 0) "demo did not create a candidate artifact"
    return $demo
}

function Get-SmokeReviewableJob {
    param(
        [string]$BaseUrl,
        [string]$AppJobId
    )

    $job = Invoke-RestMethod -Method Get -Uri ($BaseUrl + "/pet-generation-jobs/" + $AppJobId) -TimeoutSec 5
    Assert-SmokeCondition ($job.progressStatus -eq "waiting-for-review") "job did not reach waiting-for-review"
    Assert-SmokeCondition ($job.nextAction -eq "human-review") "job did not request human review"

    $candidate = @($job.artifacts | Where-Object { $_.kind -eq "candidate" })[0]
    Assert-SmokeCondition ($null -ne $candidate) "candidate artifact missing"
    Assert-SmokeCondition (-not [string]::IsNullOrWhiteSpace([string]$candidate.downloadId)) "candidate downloadId missing"
    Assert-SmokeCondition (-not [string]::IsNullOrWhiteSpace([string]$candidate.downloadUrl)) "candidate downloadUrl missing"

    return [pscustomobject]@{
        job = $job
        candidate = $candidate
    }
}

function Test-SmokePackageDownloadBlocked {
    param(
        [System.Net.WebClient]$WebClient,
        [string]$BaseUrl,
        [string]$AppJobId
    )

    try {
        $WebClient.DownloadData($BaseUrl + "/pet-generation-jobs/" + $AppJobId + "/package") | Out-Null
        return $false
    } catch [System.Net.WebException] {
        return $_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 409
    }
}

function Submit-SmokeReviewDecision {
    param(
        [string]$BaseUrl,
        [string]$AppJobId,
        [string]$DecisionId,
        [string]$Decision,
        [string]$TargetDownloadId,
        [string[]]$Notes
    )

    $decisionBody = @{
        schema = "fantasy-pet.review-decision.v1"
        decisionId = $DecisionId
        reviewer = "human-review"
        decision = $Decision
        targetDownloadId = $TargetDownloadId
        stage = "human-review"
        notes = $Notes
    } | ConvertTo-Json -Depth 8

    return Invoke-RestMethod -Method Post -Uri ($BaseUrl + "/pet-generation-jobs/" + $AppJobId + "/review-decisions") -ContentType "application/json" -Body $decisionBody -TimeoutSec 10
}

$gamerRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($FantasyPetRuleRoot)) {
    if (-not [string]::IsNullOrWhiteSpace($env:FANTASY_PET_RULE_ROOT)) {
        $FantasyPetRuleRoot = $env:FANTASY_PET_RULE_ROOT
    } else {
        $FantasyPetRuleRoot = Join-Path (Split-Path -Parent $gamerRoot) "fantasy-pet-rule"
    }
}

Assert-SmokeCondition (Test-Path -LiteralPath $FantasyPetRuleRoot) "fantasy-pet-rule root not found: $FantasyPetRuleRoot"
Assert-SmokeCondition (Test-Path -LiteralPath (Join-Path $FantasyPetRuleRoot "tools\app_server.py")) "tools\app_server.py not found under $FantasyPetRuleRoot"
Assert-SmokeCondition (Test-Path -LiteralPath (Join-Path $FantasyPetRuleRoot "tools\run_server_job_lifecycle_demo.py")) "tools\run_server_job_lifecycle_demo.py not found under $FantasyPetRuleRoot"

$safeRunId = ($AppJobId -replace "[^A-Za-z0-9._-]+", "-").Trim("-")
if ([string]::IsNullOrWhiteSpace($safeRunId)) {
    $safeRunId = "public-lifecycle-smoke"
}

$acceptAppJobId = $AppJobId
$reviseAppJobId = "$safeRunId-revise"
$rejectAppJobId = "$safeRunId-reject"

$runRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("fantasy-pet-public-lifecycle-" + [System.Guid]::NewGuid().ToString("N"))
$serverProcess = $null
$webClient = New-Object System.Net.WebClient

try {
    New-Item -ItemType Directory -Path $runRoot | Out-Null

    Invoke-SmokeLifecycleDemo `
        -FantasyPetRuleRoot $FantasyPetRuleRoot `
        -RunRoot $runRoot `
        -AppJobId $acceptAppJobId `
        -RunId $safeRunId `
        -Description $Description `
        -BodyShape $BodyShape | Out-Null
    Invoke-SmokeLifecycleDemo `
        -FantasyPetRuleRoot $FantasyPetRuleRoot `
        -RunRoot $runRoot `
        -AppJobId $reviseAppJobId `
        -RunId $reviseAppJobId `
        -Description "$Description Revise-path smoke candidate." `
        -BodyShape $BodyShape | Out-Null
    Invoke-SmokeLifecycleDemo `
        -FantasyPetRuleRoot $FantasyPetRuleRoot `
        -RunRoot $runRoot `
        -AppJobId $rejectAppJobId `
        -RunId $rejectAppJobId `
        -Description "$Description Reject-path smoke candidate." `
        -BodyShape $BodyShape | Out-Null

    $port = Get-SmokeFreePort
    $baseUrl = "http://127.0.0.1:$port"
    $serverArgs = @(
        "run",
        "--with-requirements",
        "requirements-server.txt",
        "python",
        "tools\app_server.py",
        "--run-root",
        $runRoot,
        "--host",
        "127.0.0.1",
        "--port",
        [string]$port
    )
    $serverProcess = Start-Process -FilePath "uv" -ArgumentList $serverArgs -WorkingDirectory $FantasyPetRuleRoot -WindowStyle Hidden -PassThru

    $contract = $null
    $lastError = $null
    for ($attempt = 0; $attempt -lt 80; $attempt++) {
        Assert-SmokeCondition (-not $serverProcess.HasExited) "public app server exited before readiness"
        try {
            $contract = Invoke-RestMethod -Method Get -Uri ($baseUrl + "/app-api-contract") -TimeoutSec 3
            break
        } catch {
            $lastError = $_.Exception.Message
            Start-Sleep -Milliseconds 250
        }
    }
    Assert-SmokeCondition ($null -ne $contract) "public app server was not ready: $lastError"
    Assert-SmokeAppApiContract -Contract $contract

    $workerReadiness = Invoke-RestMethod -Method Get -Uri ($baseUrl + "/worker-readiness") -TimeoutSec 5
    Assert-SmokeWorkerReadiness -Readiness $workerReadiness

    $acceptReview = Get-SmokeReviewableJob -BaseUrl $baseUrl -AppJobId $acceptAppJobId
    $job = $acceptReview.job
    $candidate = $acceptReview.candidate

    $previewBytes = $webClient.DownloadData($baseUrl + [string]$candidate.downloadUrl)
    Assert-SmokeCondition ($previewBytes.Length -gt 0) "candidate preview download returned no bytes"
    $previewImage = Assert-SmokeImageDecodes -Bytes $previewBytes -Label "candidate preview"

    $packageBlocked = Test-SmokePackageDownloadBlocked -WebClient $webClient -BaseUrl $baseUrl -AppJobId $acceptAppJobId
    Assert-SmokeCondition $packageBlocked "package download was not blocked before human accept"

    $accepted = Submit-SmokeReviewDecision `
        -BaseUrl $baseUrl `
        -AppJobId $acceptAppJobId `
        -DecisionId "decision-$safeRunId-accept" `
        -Decision "accept" `
        -TargetDownloadId ([string]$candidate.downloadId) `
        -Notes @("User visually accepted this demo candidate in the public lifecycle smoke.")
    Assert-SmokeCondition ($accepted.downloadReady -eq $true) "downloadReady was not true after human accept"
    Assert-SmokeCondition ($accepted.nextAction -eq "download-package") "nextAction was not download-package after human accept"

    $reviseReview = Get-SmokeReviewableJob -BaseUrl $baseUrl -AppJobId $reviseAppJobId
    $reviseNotes = @("idle action jumps vertically between frames 1 and 2")
    $revised = Submit-SmokeReviewDecision `
        -BaseUrl $baseUrl `
        -AppJobId $reviseAppJobId `
        -DecisionId "decision-$safeRunId-revise" `
        -Decision "revise" `
        -TargetDownloadId ([string]$reviseReview.candidate.downloadId) `
        -Notes $reviseNotes
    Assert-SmokeCondition ($revised.progressStatus -eq "revision-requested") "revise did not produce revision-requested"
    Assert-SmokeCondition ($revised.nextAction -eq "await-revision") "revise did not produce await-revision"
    Assert-SmokeCondition ($revised.downloadReady -eq $false) "revise unexpectedly enabled download"
    $packageBlockedAfterRevise = Test-SmokePackageDownloadBlocked -WebClient $webClient -BaseUrl $baseUrl -AppJobId $reviseAppJobId
    Assert-SmokeCondition $packageBlockedAfterRevise "package download was not blocked after revise"

    $rejectReview = Get-SmokeReviewableJob -BaseUrl $baseUrl -AppJobId $rejectAppJobId
    $rejectNotes = @("running-right is nearly static and needs visible alternating gait")
    $rejected = Submit-SmokeReviewDecision `
        -BaseUrl $baseUrl `
        -AppJobId $rejectAppJobId `
        -DecisionId "decision-$safeRunId-reject" `
        -Decision "reject" `
        -TargetDownloadId ([string]$rejectReview.candidate.downloadId) `
        -Notes $rejectNotes
    Assert-SmokeCondition ($rejected.progressStatus -eq "candidate-rejected") "reject did not produce candidate-rejected"
    Assert-SmokeCondition ($rejected.nextAction -eq "await-new-candidate") "reject did not produce await-new-candidate"
    Assert-SmokeCondition ($rejected.downloadReady -eq $false) "reject unexpectedly enabled download"
    $packageBlockedAfterReject = Test-SmokePackageDownloadBlocked -WebClient $webClient -BaseUrl $baseUrl -AppJobId $rejectAppJobId
    Assert-SmokeCondition $packageBlockedAfterReject "package download was not blocked after reject"

    $packageBytes = $webClient.DownloadData($baseUrl + "/pet-generation-jobs/" + $acceptAppJobId + "/package")
    Assert-SmokeCondition ($packageBytes.Length -gt 0) "package download returned no bytes"

    $manifestText = Read-SmokeZipEntryText -ZipBytes $packageBytes -EntryName "package-manifest.json"
    $packageManifest = $manifestText | ConvertFrom-Json
    Assert-SmokeCondition ($packageManifest.schema -eq "fantasy-pet.package-manifest.v1") "unexpected package manifest schema"
    Assert-SmokeCondition ($packageManifest.acceptedBy -eq "human-review") "package manifest was not human reviewed"
    Assert-SmokeCondition ([string]$packageManifest.sourceDownloadId -eq [string]$candidate.downloadId) "package manifest sourceDownloadId mismatch"
    $packageManifestFiles = @($packageManifest.files)
    Assert-SmokeCondition ($packageManifestFiles.Count -gt 0) "package manifest files missing"
    $packageManifestCandidateFiles = @($packageManifestFiles | Where-Object { $_.kind -eq "candidate" })
    Assert-SmokeCondition ($packageManifestCandidateFiles.Count -gt 0) "package manifest candidate file missing"
    $unsafePackageManifestFiles = @(
        $packageManifestFiles | Where-Object {
            -not (Test-SmokeSafePackageRelativePath -Path ([string]$_.path))
        }
    )
    Assert-SmokeCondition ($unsafePackageManifestFiles.Count -eq 0) "package manifest contains unsafe file paths"

    $publicJson = (@($job, $accepted, $reviseReview.job, $revised, $rejectReview.job, $rejected) | ConvertTo-Json -Depth 30)
    $forbidden = @(Get-SmokeInternalMarkers) + @("/admin/")
    $leaked = @($forbidden | Where-Object { $publicJson.Contains($_) })
    Assert-SmokeCondition ($leaked.Count -eq 0) ("public lifecycle response leaked: " + ($leaked -join ","))

    [pscustomobject]@{
        schema = "gamer.fantasy-pet-public-lifecycle-smoke.v1"
        status = "passed"
        appJobId = $AppJobId
        contractSchema = $contract.schema
        workerReadinessSchema = $workerReadiness.schema
        workerReadinessStatus = $workerReadiness.status
        initialProgressStatus = $job.progressStatus
        candidateDownloadId = $candidate.downloadId
        previewBytes = $previewBytes.Length
        previewWidth = $previewImage.width
        previewHeight = $previewImage.height
        blockedPackageBeforeAccept = $packageBlocked
        acceptedProgressStatus = $accepted.progressStatus
        acceptedNextAction = $accepted.nextAction
        downloadReady = $accepted.downloadReady
        revisionProgressStatus = $revised.progressStatus
        revisionNextAction = $revised.nextAction
        packageBlockedAfterRevise = $packageBlockedAfterRevise
        rejectionProgressStatus = $rejected.progressStatus
        rejectionNextAction = $rejected.nextAction
        packageBlockedAfterReject = $packageBlockedAfterReject
        reviseNotesCount = $reviseNotes.Count
        rejectNotesCount = $rejectNotes.Count
        packageBytes = $packageBytes.Length
        packageManifestSchema = $packageManifest.schema
        packageManifestAcceptedBy = $packageManifest.acceptedBy
        packageManifestSourceDownloadId = $packageManifest.sourceDownloadId
        packageManifestCandidateFileCount = $packageManifestCandidateFiles.Count
        runRoot = if ($KeepRunRoot) { $runRoot } else { "" }
    } | ConvertTo-Json -Depth 8
} finally {
    $webClient.Dispose()
    Stop-SmokeServerProcesses -RunRoot $runRoot
    if ($serverProcess -and -not $serverProcess.HasExited) {
        Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue
    }
    if (-not $KeepRunRoot) {
        Remove-SmokeRunRoot -RunRoot $runRoot
    }
}
