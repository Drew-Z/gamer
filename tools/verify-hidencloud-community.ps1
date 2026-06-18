param(
  [string]$BaseUrl = "http://olivia.hidencloud.com:24674",
  [string]$PetId = "pet-stardust-001",
  [string]$ExpectedAppJobId = "issue-1-fresh-timeout3600-20260610-1",
  [string]$ExpectedCommit = "",
  [int]$TimeoutSec = 60
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Net.Http

function New-HttpClient {
  $client = [System.Net.Http.HttpClient]::new()
  $client.Timeout = [TimeSpan]::FromSeconds($TimeoutSec)
  return $client
}

function Resolve-Uri {
  param(
    [string]$Path
  )

  $base = [Uri]::new($BaseUrl.TrimEnd("/") + "/")
  return [Uri]::new($base, $Path.TrimStart("/"))
}

function Get-Json {
  param(
    [System.Net.Http.HttpClient]$Client,
    [string]$Path
  )

  $uri = Resolve-Uri -Path $Path
  $response = $Client.GetAsync($uri).GetAwaiter().GetResult()
  $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()

  if (-not $response.IsSuccessStatusCode) {
    throw "GET $uri failed with $([int]$response.StatusCode): $body"
  }

  return $body | ConvertFrom-Json
}

function Get-ArtifactSummary {
  param(
    [System.Net.Http.HttpClient]$Client,
    [string]$Path
  )

  $uri = Resolve-Uri -Path $Path
  $response = $Client.GetAsync($uri).GetAwaiter().GetResult()
  $bytes = $response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
  $contentType = ""

  if ($response.Content.Headers.ContentType) {
    $contentType = $response.Content.Headers.ContentType.MediaType
  }

  if (-not $response.IsSuccessStatusCode) {
    throw "GET $uri failed with $([int]$response.StatusCode), $($bytes.Length) bytes"
  }

  return [pscustomobject]@{
    statusCode = [int]$response.StatusCode
    contentType = $contentType
    byteCount = $bytes.Length
  }
}

function Assert-Condition {
  param(
    [bool]$Condition,
    [string]$Message
  )

  if (-not $Condition) {
    throw $Message
  }
}

function Resolve-ExpectedCommit {
  param(
    [string]$ExplicitCommit
  )

  if ($ExplicitCommit.Trim() -ne "") {
    return $ExplicitCommit.Trim()
  }

  $repoRoot = Split-Path -Parent $PSScriptRoot
  $headFromFiles = Resolve-GitHeadCommit -RepoRoot $repoRoot
  if ($headFromFiles.Trim() -ne "") {
    return $headFromFiles.Trim()
  }

  try {
    $head = git -C $repoRoot rev-parse --short HEAD 2>$null
    if ($LASTEXITCODE -eq 0 -and $head.Trim() -ne "") {
      return $head.Trim()
    }
  } catch {
    return ""
  }

  return ""
}

function Resolve-GitHeadCommit {
  param(
    [string]$RepoRoot
  )

  $gitPath = Join-Path $RepoRoot ".git"
  if (-not (Test-Path -LiteralPath $gitPath)) {
    return ""
  }

  $gitDir = $gitPath
  if (-not (Get-Item -LiteralPath $gitPath -Force).PSIsContainer) {
    $gitFile = (Get-Content -LiteralPath $gitPath -Raw).Trim()
    if ($gitFile.StartsWith("gitdir:")) {
      $gitDir = [System.IO.Path]::GetFullPath(
        (Join-Path $RepoRoot $gitFile.Substring("gitdir:".Length).Trim())
      )
    }
  }

  $headPath = Join-Path $gitDir "HEAD"
  if (-not (Test-Path -LiteralPath $headPath)) {
    return ""
  }

  $head = (Get-Content -LiteralPath $headPath -Raw).Trim()
  if ($head -match "^[a-fA-F0-9]{7,40}$") {
    return $head.Substring(0, [Math]::Min(7, $head.Length))
  }

  if (-not $head.StartsWith("ref:")) {
    return ""
  }

  $refName = $head.Substring("ref:".Length).Trim()
  $refPath = Join-Path $gitDir $refName
  if (Test-Path -LiteralPath $refPath) {
    $refCommit = (Get-Content -LiteralPath $refPath -Raw).Trim()
    if ($refCommit -match "^[a-fA-F0-9]{7,40}$") {
      return $refCommit.Substring(0, [Math]::Min(7, $refCommit.Length))
    }
  }

  $packedRefsPath = Join-Path $gitDir "packed-refs"
  if (Test-Path -LiteralPath $packedRefsPath) {
    foreach ($line in Get-Content -LiteralPath $packedRefsPath) {
      $parts = $line.Trim() -split "\s+"
      if ($parts.Count -ge 2 -and $parts[1] -eq $refName -and $parts[0] -match "^[a-fA-F0-9]{7,40}$") {
        return $parts[0].Substring(0, [Math]::Min(7, $parts[0].Length))
      }
    }
  }

  return ""
}

$client = New-HttpClient
$expectedReleaseCommit = Resolve-ExpectedCommit -ExplicitCommit $ExpectedCommit

try {
  $health = Get-Json -Client $client -Path "/health"
  Assert-Condition -Condition ($health.ok -eq $true) -Message "community-api health is not ok"

  $releaseCommit = ""
  if ($health.release -and $health.release.commit) {
    $releaseCommit = [string]$health.release.commit
  }

  if ($expectedReleaseCommit.Trim() -ne "") {
    Assert-Condition `
      -Condition ($releaseCommit.StartsWith($expectedReleaseCommit.Trim())) `
      -Message "release.commit '$releaseCommit' does not start with '$expectedReleaseCommit'"
  }

  $sla = Get-Json -Client $client -Path "/v1/sla"
  Assert-Condition -Condition ($null -ne $sla) -Message "community SLA route returned no body"

  $approvedPets = Get-Json -Client $client -Path "/v1/pets/approved"
  $pet = $approvedPets.items | Where-Object { $_.petId -eq $PetId } | Select-Object -First 1
  Assert-Condition -Condition ($null -ne $pet) -Message "approved pet '$PetId' was not found"

  $previewUrl = [string]$pet.assets.previewUrl
  $targetDownloadId = [string]$pet.assets.targetDownloadId
  $appJobId = [string]$pet.source.appJobId

  Assert-Condition `
    -Condition ($previewUrl.StartsWith("/pet-generation-jobs/") -and $previewUrl.Contains("/artifacts/")) `
    -Message "approved pet previewUrl is not a public artifact route: '$previewUrl'"
  Assert-Condition -Condition ($targetDownloadId.Trim() -ne "") -Message "targetDownloadId is missing"
  Assert-Condition -Condition ($appJobId.Trim() -ne "") -Message "source.appJobId is missing"
  if ($ExpectedAppJobId.Trim() -ne "") {
    Assert-Condition `
      -Condition ($appJobId -eq $ExpectedAppJobId.Trim()) `
      -Message "source.appJobId '$appJobId' does not match '$ExpectedAppJobId'"
  }

  $directPreview = Get-ArtifactSummary -Client $client -Path $previewUrl
  Assert-Condition -Condition ($directPreview.byteCount -gt 0) -Message "direct preview returned no bytes"

  $adminPreview = Get-ArtifactSummary -Client $client -Path "/api$previewUrl"
  Assert-Condition -Condition ($adminPreview.byteCount -gt 0) -Message "admin proxy preview returned no bytes"

  [pscustomobject]@{
    ok = $true
    baseUrl = $BaseUrl
    healthService = $health.service
    releaseCommit = $releaseCommit
    expectedReleaseCommit = $expectedReleaseCommit
    sla = $sla
    petId = $pet.petId
    appJobId = $appJobId
    targetDownloadId = $targetDownloadId
    previewUrl = $previewUrl
    directPreview = $directPreview
    adminPreview = $adminPreview
  } | ConvertTo-Json -Depth 6
} finally {
  $client.Dispose()
}
