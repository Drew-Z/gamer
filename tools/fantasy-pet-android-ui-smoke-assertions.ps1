$ErrorActionPreference = "Stop"

function ConvertTo-AndroidUiHierarchyXml {
    param([string[]]$RawDumpLines)

    $rawText = ($RawDumpLines -join "`n")
    $startIndex = $rawText.IndexOf("<?xml", [System.StringComparison]::Ordinal)
    if ($startIndex -lt 0) {
        $startIndex = $rawText.IndexOf("<hierarchy", [System.StringComparison]::Ordinal)
    }

    $endMarker = "</hierarchy>"
    $endIndex = $rawText.LastIndexOf($endMarker, [System.StringComparison]::Ordinal)
    if ($startIndex -ge 0 -and $endIndex -ge $startIndex) {
        return $rawText.Substring($startIndex, $endIndex + $endMarker.Length - $startIndex)
    }

    throw "Android UI dump did not include a hierarchy XML payload."
}

function Get-AndroidUiXmlDocument {
    param([string]$UiXml)

    if ($UiXml.Trim().Length -eq 0) {
        throw "Android UI XML is empty."
    }

    [xml]$document = $UiXml
    Write-Output -NoEnumerate $document
}

function Get-AndroidUiNodes {
    param([xml]$Document)

    return @($Document.SelectNodes("//node"))
}

function Get-AndroidUiAttribute {
    param(
        [System.Xml.XmlNode]$Node,
        [string]$Name
    )

    $attribute = $Node.Attributes[$Name]
    if ($null -eq $attribute) {
        return ""
    }
    return [string]$attribute.Value
}

function Test-AndroidUiHasTextFragment {
    param(
        [string]$UiXml,
        [string]$TextFragment
    )

    $document = Get-AndroidUiXmlDocument -UiXml $UiXml
    foreach ($node in Get-AndroidUiNodes -Document $document) {
        $text = Get-AndroidUiAttribute -Node $node -Name "text"
        if ($text.Contains($TextFragment)) {
            return $true
        }
    }
    return $false
}

function Get-AndroidUiActionNodeByContentDescription {
    param(
        [string]$UiXml,
        [string]$ContentDescription
    )

    $document = Get-AndroidUiXmlDocument -UiXml $UiXml
    foreach ($node in Get-AndroidUiNodes -Document $document) {
        $contentDescription = Get-AndroidUiAttribute -Node $node -Name "content-desc"
        if ($contentDescription -eq $ContentDescription) {
            Write-Output -NoEnumerate $node.ParentNode
            return
        }
    }
    return $null
}

function Test-AndroidUiActionDisabled {
    param(
        [string]$UiXml,
        [string]$ContentDescription
    )

    $document = Get-AndroidUiXmlDocument -UiXml $UiXml
    foreach ($node in Get-AndroidUiNodes -Document $document) {
        $nodeContentDescription = Get-AndroidUiAttribute -Node $node -Name "content-desc"
        if ($nodeContentDescription -eq $ContentDescription) {
            $actionNode = $node.ParentNode
            return (Get-AndroidUiAttribute -Node $actionNode -Name "enabled") -eq "false"
        }
    }

    return $false
}

function Test-AndroidUiHasContentDescription {
    param(
        [string]$UiXml,
        [string]$ContentDescription
    )

    $document = Get-AndroidUiXmlDocument -UiXml $UiXml
    foreach ($node in Get-AndroidUiNodes -Document $document) {
        $nodeContentDescription = Get-AndroidUiAttribute -Node $node -Name "content-desc"
        if ($nodeContentDescription -eq $ContentDescription) {
            return $true
        }
    }
    return $false
}

function ConvertFrom-AndroidUiBounds {
    param([string]$Bounds)

    $match = [regex]::Match($Bounds, '^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')
    if (-not $match.Success) {
        throw "Invalid Android UI bounds: $Bounds"
    }

    $left = [int]$match.Groups[1].Value
    $top = [int]$match.Groups[2].Value
    $right = [int]$match.Groups[3].Value
    $bottom = [int]$match.Groups[4].Value
    return [pscustomobject]@{
        left = $left
        top = $top
        right = $right
        bottom = $bottom
        x = [int](($left + $right) / 2)
        y = [int](($top + $bottom) / 2)
    }
}

function Get-AndroidUiCenterByTextFragment {
    param(
        [string]$UiXml,
        [string]$TextFragment
    )

    $document = Get-AndroidUiXmlDocument -UiXml $UiXml
    foreach ($node in Get-AndroidUiNodes -Document $document) {
        $text = Get-AndroidUiAttribute -Node $node -Name "text"
        if ($text.Contains($TextFragment)) {
            $bounds = ConvertFrom-AndroidUiBounds -Bounds (Get-AndroidUiAttribute -Node $node -Name "bounds")
            return [pscustomobject]@{
                x = $bounds.x
                y = $bounds.y
            }
        }
    }

    throw "Android UI text fragment not found: $TextFragment"
}

function Get-AndroidUiCenterByContentDescription {
    param(
        [string]$UiXml,
        [string]$ContentDescription
    )

    $document = Get-AndroidUiXmlDocument -UiXml $UiXml
    foreach ($node in Get-AndroidUiNodes -Document $document) {
        $nodeContentDescription = Get-AndroidUiAttribute -Node $node -Name "content-desc"
        if ($nodeContentDescription -eq $ContentDescription) {
            $bounds = ConvertFrom-AndroidUiBounds -Bounds (Get-AndroidUiAttribute -Node $node -Name "bounds")
            return [pscustomobject]@{
                x = $bounds.x
                y = $bounds.y
            }
        }
    }

    throw "Android UI content description not found: $ContentDescription"
}

function Test-ContractDemoAndroidUiState {
    param([string]$UiXml)

    $publicApiBoundaryNoticeVisible = (
        (Test-AndroidUiHasContentDescription `
            -UiXml $UiXml `
            -ContentDescription "generation-public-api-boundary-notice") -or
        (Test-AndroidUiHasTextFragment `
            -UiXml $UiXml `
            -TextFragment "公共 API 只创建和轮询任务") -or
        (Test-AndroidUiHasTextFragment `
            -UiXml $UiXml `
            -TextFragment "Public API only creates and polls jobs")
    )
    $contractDemoWarningVisible = (
        (Test-AndroidUiHasContentDescription `
            -UiXml $UiXml `
            -ContentDescription "generation-contract-demo-notice") -or
        (Test-AndroidUiHasTextFragment `
            -UiXml $UiXml `
            -TextFragment "这是公共 API 契约演示任务") -or
        (Test-AndroidUiHasTextFragment `
            -UiXml $UiXml `
            -TextFragment "Contract demo task")
    )
    $contractDemoNoLiveWorkerVisible = (
        (Test-AndroidUiHasContentDescription `
            -UiXml $UiXml `
            -ContentDescription "generation-contract-demo-no-live-worker") -or
        (Test-AndroidUiHasTextFragment `
            -UiXml $UiXml `
            -TextFragment "真实生成 worker 尚未运行") -or
        (Test-AndroidUiHasTextFragment `
            -UiXml $UiXml `
            -TextFragment "no live generation worker has run")
    )
    $reviewAcceptDisabled = Test-AndroidUiActionDisabled `
        -UiXml $UiXml `
        -ContentDescription "generation-review-accept-button"
    $packageDownloadDisabled = Test-AndroidUiActionDisabled `
        -UiXml $UiXml `
        -ContentDescription "generation-package-download-button"
    $passed = $publicApiBoundaryNoticeVisible -and
        $contractDemoWarningVisible -and
        $contractDemoNoLiveWorkerVisible -and
        $reviewAcceptDisabled -and
        $packageDownloadDisabled

    return [pscustomobject]@{
        passed = [bool]$passed
        publicApiBoundaryNoticeVisible = [bool]$publicApiBoundaryNoticeVisible
        contractDemoWarningVisible = [bool]$contractDemoWarningVisible
        contractDemoNoLiveWorkerVisible = [bool]$contractDemoNoLiveWorkerVisible
        reviewAcceptDisabled = [bool]$reviewAcceptDisabled
        packageDownloadDisabled = [bool]$packageDownloadDisabled
    }
}

function Assert-ContractDemoAndroidUiState {
    param([string]$UiXml)

    $result = Test-ContractDemoAndroidUiState -UiXml $UiXml
    $missing = @()
    if (-not $result.publicApiBoundaryNoticeVisible) {
        $missing += "public API boundary notice"
    }
    if (-not $result.contractDemoWarningVisible) {
        $missing += "contract demo warning text"
    }
    if (-not $result.contractDemoNoLiveWorkerVisible) {
        $missing += "contract demo no live generation worker text"
    }
    if (-not $result.reviewAcceptDisabled) {
        $missing += "generation-review-accept-button disabled"
    }
    if (-not $result.packageDownloadDisabled) {
        $missing += "generation-package-download-button disabled"
    }
    if ($missing.Count -gt 0) {
        throw "Contract demo Android UI assertion failed: $($missing -join '; ')"
    }

    return $result
}
