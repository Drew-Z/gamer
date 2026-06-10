param(
  [string]$BaseUrl = "http://olivia.hidencloud.com:24674",
  [string]$PetId = "issue-1-fresh-timeout3600-20260610-1",
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

$client = New-HttpClient

try {
  $health = Get-Json -Client $client -Path "/health"
  Assert-Condition -Condition ($health.ok -eq $true) -Message "community-api health is not ok"

  $releaseCommit = ""
  if ($health.release -and $health.release.commit) {
    $releaseCommit = [string]$health.release.commit
  }

  if ($ExpectedCommit.Trim() -ne "") {
    Assert-Condition `
      -Condition ($releaseCommit.StartsWith($ExpectedCommit.Trim())) `
      -Message "release.commit '$releaseCommit' does not start with '$ExpectedCommit'"
  }

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

  $directPreview = Get-ArtifactSummary -Client $client -Path $previewUrl
  Assert-Condition -Condition ($directPreview.byteCount -gt 0) -Message "direct preview returned no bytes"

  $adminPreview = Get-ArtifactSummary -Client $client -Path "/api$previewUrl"
  Assert-Condition -Condition ($adminPreview.byteCount -gt 0) -Message "admin proxy preview returned no bytes"

  [pscustomobject]@{
    ok = $true
    baseUrl = $BaseUrl
    healthService = $health.service
    releaseCommit = $releaseCommit
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
