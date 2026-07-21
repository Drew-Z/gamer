param(
    [string]$FantasyPetRuleRoot = "",
    [string]$GradleWrapper = "D:\workspace4Cursor\pet\floating-pet-android\gradlew.bat",
    [string]$RunRoot = "",
    [string]$AppJobId = "publicdemo1",
    [string]$Description = "A tiny stardust dragon desktop pet with smooth idle motion.",
    [ValidateSet("balanced", "wide", "wide-tail", "tall")]
    [string]$BodyShape = "wide-tail",
    [string]$EmulatorSerial = "emulator-5554",
    [string]$AdbPath = "adb",
    [string]$FantasyPetApiBaseUrl = "http://10.0.2.2:8765",
    [string]$CommunityApiBaseUrl = "http://10.0.2.2:4000",
    [int]$PublicApiPort = 8765,
    [switch]$StartPublicApi,
    [switch]$SkipSeed,
    [switch]$SkipInstall,
    [switch]$SkipClear,
    [switch]$SkipLaunch,
    [switch]$CaptureScreenshot,
    [switch]$AssertContractDemoUi,
    [switch]$StopPublicApiOnExit,
    [string]$ScreenshotDirectory = "",
    [int]$ScreenshotDelaySeconds = 2
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "fantasy-pet-android-ui-smoke-assertions.ps1")

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

function Resolve-DefaultFantasyPetRuleRoot {
    $gamerRoot = Split-Path -Parent $PSScriptRoot
    $workspaceRoot = Split-Path -Parent $gamerRoot
    return Join-Path $workspaceRoot "fantasy-pet-rule"
}

function Resolve-AndroidRoot {
    $gamerRoot = Split-Path -Parent $PSScriptRoot
    return Join-Path $gamerRoot "apps\android-community"
}

function Reset-TempDemoRunDirectory {
    param([string]$Path)

    $tempRoot = (Resolve-Path -LiteralPath ([System.IO.Path]::GetTempPath())).Path
    $parent = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent | Out-Null
    }

    $resolvedParent = (Resolve-Path -LiteralPath $parent).Path
    if (-not $resolvedParent.StartsWith($tempRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        Write-Host "Skipping demo run reset outside temp: $Path"
        return
    }

    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
}

function Wait-PublicApiContract {
    param(
        [string]$BaseUrl,
        [int]$MaxAttempts = 20
    )

    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt += 1) {
        try {
            Invoke-RestMethod -Method Get -Uri ($BaseUrl + "/app-api-contract") -TimeoutSec 2 | Out-Null
            return
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }

    throw "Public fantasy pet API did not become ready at $BaseUrl"
}

function Stop-PublicFantasyPetApi {
    param(
        [System.Diagnostics.Process]$Process,
        [int]$Port,
        [string]$RunRoot
    )

    Write-Host ""
    Write-Host "== Stop public fantasy pet API"
    if ($Process -and -not $Process.HasExited) {
        Stop-Process -Id $Process.Id -Force
    }

    $connections = @(
        Get-NetTCPConnection `
            -LocalPort $Port `
            -State Listen `
            -ErrorAction SilentlyContinue
    )
    foreach ($connection in $connections) {
        $serverProcess = Get-CimInstance `
            -ClassName Win32_Process `
            -Filter "ProcessId = $($connection.OwningProcess)" `
            -ErrorAction SilentlyContinue
        $commandLine = [string]$serverProcess.CommandLine
        if (
            $commandLine.IndexOf("tools\app_server.py", [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -and
            $commandLine.IndexOf($RunRoot, [System.StringComparison]::OrdinalIgnoreCase) -ge 0
        ) {
            Stop-Process -Id $connection.OwningProcess -Force
        }
    }
}

function Invoke-AdbStep {
    param(
        [string]$Name,
        [string[]]$ArgumentList
    )

    Write-Host ""
    Write-Host "== $Name"
    & $AdbPath @ArgumentList
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        throw "$Name failed with exit code $exitCode"
    }
}

function Invoke-AndroidScreenshotCapture {
    param(
        [string]$OutputDirectory,
        [string]$OutputFileName
    )

    if (-not (Test-Path -LiteralPath $OutputDirectory)) {
        New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
    }

    $deviceScreenshotPath = "/sdcard/Download/$OutputFileName"
    $localScreenshotPath = Join-Path $OutputDirectory $OutputFileName
    if ($ScreenshotDelaySeconds -gt 0) {
        Start-Sleep -Seconds $ScreenshotDelaySeconds
    }

    Invoke-AdbStep -Name "Capture Android screenshot" -ArgumentList @(
        "-s",
        $EmulatorSerial,
        "shell",
        "screencap",
        "-p",
        $deviceScreenshotPath
    )
    Invoke-AdbStep -Name "Pull Android screenshot" -ArgumentList @(
        "-s",
        $EmulatorSerial,
        "pull",
        $deviceScreenshotPath,
        $localScreenshotPath
    )

    if (-not (Test-Path -LiteralPath $localScreenshotPath)) {
        throw "Screenshot pull did not create $localScreenshotPath"
    }
    return (Resolve-Path -LiteralPath $localScreenshotPath).Path
}

function Test-ScreenshotLikelyBlank {
    param([string]$Path)

    Add-Type -AssemblyName System.Drawing
    $bitmap = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        $sampleCount = 0
        $nonDarkCount = 0
        $stepX = [Math]::Max(1, [int]($bitmap.Width / 24))
        $stepY = [Math]::Max(1, [int]($bitmap.Height / 24))

        for ($x = 0; $x -lt $bitmap.Width; $x += $stepX) {
            for ($y = 0; $y -lt $bitmap.Height; $y += $stepY) {
                $pixel = $bitmap.GetPixel($x, $y)
                $sampleCount += 1
                if (($pixel.R + $pixel.G + $pixel.B) -gt 45) {
                    $nonDarkCount += 1
                }
            }
        }

        if ($sampleCount -eq 0) {
            return $true
        }

        return (($nonDarkCount / $sampleCount) -lt 0.02)
    } finally {
        $bitmap.Dispose()
    }
}

function Get-AndroidUiDumpXml {
    param([int]$MaxAttempts = 5)

    Write-Host ""
    Write-Host "== Dump Android UI tree"
    $lastFailure = "Android UI dump did not include a hierarchy XML payload."
    $deviceUiPath = "/sdcard/window.xml"
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt += 1) {
        $rawDump = & $AdbPath @(
            "-s",
            $EmulatorSerial,
            "exec-out",
            "uiautomator",
            "dump",
            "/dev/tty"
        )
        $exitCode = $LASTEXITCODE
        if ($exitCode -ne 0) {
            $lastFailure = "Dump Android UI tree failed with exit code $exitCode"
        } else {
            try {
                return ConvertTo-AndroidUiHierarchyXml -RawDumpLines $rawDump
            } catch {
                $lastFailure = $_.Exception.Message
            }
        }

        $deviceDump = & $AdbPath @(
            "-s",
            $EmulatorSerial,
            "shell",
            "uiautomator",
            "dump",
            $deviceUiPath
        )
        $deviceDumpExitCode = $LASTEXITCODE
        if ($deviceDumpExitCode -ne 0) {
            $lastFailure = "Dump Android UI tree to $deviceUiPath failed with exit code $deviceDumpExitCode"
        } else {
            $rawDeviceDump = & $AdbPath @(
                "-s",
                $EmulatorSerial,
                "exec-out",
                "cat",
                $deviceUiPath
            )
            $catExitCode = $LASTEXITCODE
            if ($catExitCode -ne 0) {
                $lastFailure = "Read Android UI tree from $deviceUiPath failed with exit code $catExitCode"
            } else {
                try {
                    return ConvertTo-AndroidUiHierarchyXml -RawDumpLines $rawDeviceDump
                } catch {
                    $lastFailure = $_.Exception.Message
                }
            }
        }

        if ($attempt -lt $MaxAttempts) {
            Start-Sleep -Milliseconds (300 * $attempt)
        }
    }

    throw $lastFailure
}

function Join-AndroidUiSnapshotXml {
    param([string[]]$Snapshots)

    $innerNodes = New-Object System.Collections.Generic.List[string]
    foreach ($snapshot in $Snapshots) {
        $match = [regex]::Match($snapshot, '<hierarchy[^>]*>([\s\S]*)</hierarchy>')
        if ($match.Success) {
            $innerNodes.Add($match.Groups[1].Value)
        }
    }

    return "<hierarchy>$($innerNodes -join '')</hierarchy>"
}

function Invoke-AndroidTapCenter {
    param(
        [string]$Name,
        [pscustomobject]$Center
    )

    Invoke-AdbStep -Name $Name -ArgumentList @(
        "-s",
        $EmulatorSerial,
        "shell",
        "input",
        "tap",
        [string]$Center.x,
        [string]$Center.y
    )
}

function Invoke-AndroidSwipeUp {
    Invoke-AdbStep -Name "Scroll Android UI down" -ArgumentList @(
        "-s",
        $EmulatorSerial,
        "shell",
        "input",
        "swipe",
        "540",
        "2100",
        "540",
        "900",
        "500"
    )
}

function Invoke-AndroidSwipeDown {
    Invoke-AdbStep -Name "Scroll Android UI up" -ArgumentList @(
        "-s",
        $EmulatorSerial,
        "shell",
        "input",
        "swipe",
        "540",
        "900",
        "540",
        "2100",
        "500"
    )
}

function Invoke-AndroidScrollToTop {
    param([int]$SwipeCount = 6)

    for ($index = 0; $index -lt $SwipeCount; $index += 1) {
        Invoke-AndroidSwipeDown
        Start-Sleep -Milliseconds 250
    }
}

function Set-AndroidClipboardText {
    param([string]$Text)

    try {
        $rawOutput = & $AdbPath @(
            "-s",
            $EmulatorSerial,
            "shell",
            "cmd",
            "clipboard",
            "set",
            $Text
        ) 2>&1
        $exitCode = $LASTEXITCODE
    } catch {
        return $false
    }

    if ($exitCode -ne 0) {
        return $false
    }

    $outputText = ($rawOutput -join "`n")
    return (
        $outputText.IndexOf("No shell command implementation", [System.StringComparison]::OrdinalIgnoreCase) -lt 0 -and
        $outputText.IndexOf("Unknown command", [System.StringComparison]::OrdinalIgnoreCase) -lt 0 -and
        $outputText.IndexOf("Exception", [System.StringComparison]::OrdinalIgnoreCase) -lt 0
    )
}

function Invoke-AndroidPasteClipboard {
    Invoke-AdbStep -Name "Paste Android clipboard" -ArgumentList @(
        "-s",
        $EmulatorSerial,
        "shell",
        "input",
        "keyevent",
        "279"
    )
}

function ConvertTo-SharedPreferenceXmlText {
    param([string]$Text)

    $escaped = $Text.Replace("&", "&amp;")
    $escaped = $escaped.Replace("<", "&lt;")
    $escaped = $escaped.Replace(">", "&gt;")
    $escaped = $escaped.Replace('"', "&quot;")
    return $escaped.Replace("'", "&apos;")
}

function Set-AndroidGenerationJobPreference {
    param([string]$AppJobId)

    $safeAppJobId = ConvertTo-SharedPreferenceXmlText -Text $AppJobId
    $prefsXml = @"
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="appJobId">$safeAppJobId</string>
    <string name="appJobHistory">$safeAppJobId</string>
</map>
"@
    $prefsPath = Join-Path ([System.IO.Path]::GetTempPath()) "fantasy-pet-generation-$($AppJobId).xml"
    Set-Content -LiteralPath $prefsPath -Value $prefsXml -Encoding UTF8
    Invoke-AdbStep -Name "Push Android generation preferences" -ArgumentList @(
        "-s",
        $EmulatorSerial,
        "push",
        $prefsPath,
        "/data/local/tmp/fantasy-pet-generation.xml"
    )
    Invoke-AdbStep -Name "Seed Android generation preferences" -ArgumentList @(
        "-s",
        $EmulatorSerial,
        "shell",
        "run-as",
        "com.gamer.community",
        "sh",
        "-c",
        "'mkdir -p shared_prefs && cp /data/local/tmp/fantasy-pet-generation.xml shared_prefs/fantasy-pet-generation.xml'"
    )
}

function ConvertTo-AndroidInputTextArgument {
    param([string]$Text)

    return $Text.Replace(" ", "%s").Replace("-", "\-")
}

function Enter-AndroidTextCharacters {
    param(
        [string]$Name,
        [string]$Text
    )

    foreach ($character in $Text.ToCharArray()) {
        if ($character -eq "-") {
            & $AdbPath @(
                "-s",
                $EmulatorSerial,
                "shell",
                "input",
                "keyevent",
                "69"
            )
            $exitCode = $LASTEXITCODE
            if ($exitCode -ne 0) {
                throw "$Name failed while entering '-' with exit code $exitCode"
            }
            Start-Sleep -Milliseconds 60
            continue
        }

        $textArgument = ConvertTo-AndroidInputTextArgument -Text ([string]$character)
        & $AdbPath @(
            "-s",
            $EmulatorSerial,
            "shell",
            "input",
            "text",
            $textArgument
        )
        $exitCode = $LASTEXITCODE
        if ($exitCode -ne 0) {
            throw "$Name failed while entering '$character' with exit code $exitCode"
        }
        Start-Sleep -Milliseconds 60
    }
}

function Enter-AndroidText {
    param(
        [string]$Name,
        [string]$Text
    )

    if (-not $Text.Contains("-") -and (Set-AndroidClipboardText -Text $Text)) {
        Invoke-AndroidPasteClipboard
        return
    }

    Enter-AndroidTextCharacters -Name $Name -Text $Text
}

function Invoke-AndroidTapTextFragment {
    param([string]$TextFragment)

    $uiXml = Get-AndroidUiDumpXml
    $center = Get-AndroidUiCenterByTextFragment -UiXml $uiXml -TextFragment $TextFragment
    Invoke-AndroidTapCenter -Name "Tap Android UI text '$TextFragment'" -Center $center
}

function Invoke-AndroidTapContentDescription {
    param([string]$ContentDescription)

    $uiXml = Get-AndroidUiDumpXml
    $center = Get-AndroidUiCenterByContentDescription `
        -UiXml $uiXml `
        -ContentDescription $ContentDescription
    Invoke-AndroidTapCenter -Name "Tap Android UI '$ContentDescription'" -Center $center
}

function Wait-AndroidUiContentDescription {
    param(
        [string]$ContentDescription,
        [int]$MaxAttempts = 16,
        [int]$DelayMilliseconds = 500
    )

    $lastFailure = "Android UI content description not found: $ContentDescription"
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt += 1) {
        try {
            $uiXml = Get-AndroidUiDumpXml -MaxAttempts 2
            if (Test-AndroidUiHasContentDescription `
                -UiXml $uiXml `
                -ContentDescription $ContentDescription
            ) {
                return $uiXml
            }
        } catch {
            $lastFailure = $_.Exception.Message
        }

        if ($attempt -lt $MaxAttempts) {
            Start-Sleep -Milliseconds $DelayMilliseconds
        }
    }

    throw $lastFailure
}

function Get-AndroidDisplayHeight {
    $wmSize = & $AdbPath @(
        "-s",
        $EmulatorSerial,
        "shell",
        "wm",
        "size"
    )
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        return 0
    }

    $match = [regex]::Match(($wmSize -join "`n"), '(\d+)x(\d+)')
    if (-not $match.Success) {
        return 0
    }

    return [int]$match.Groups[2].Value
}

function Get-AndroidDisplayWidth {
    $wmSize = & $AdbPath @(
        "-s",
        $EmulatorSerial,
        "shell",
        "wm",
        "size"
    )
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        return 0
    }

    $match = [regex]::Match(($wmSize -join "`n"), '(\d+)x(\d+)')
    if (-not $match.Success) {
        return 0
    }

    return [int]$match.Groups[1].Value
}

function Get-AndroidUiMaximumBottom {
    param([string]$UiXml)

    $maximumBottom = 0
    foreach ($match in [regex]::Matches($UiXml, '\[\d+,\d+\]\[\d+,(\d+)\]')) {
        $bottom = [int]$match.Groups[1].Value
        if ($bottom -gt $maximumBottom) {
            $maximumBottom = $bottom
        }
    }

    return $maximumBottom
}

function Test-AndroidUiHasFocusedTextInput {
    param([string]$UiXml)

    try {
        $document = Get-AndroidUiXmlDocument -UiXml $UiXml
        foreach ($node in Get-AndroidUiNodes -Document $document) {
            $focused = Get-AndroidUiAttribute -Node $node -Name "focused"
            if ($focused -ne "true") {
                continue
            }

            $className = Get-AndroidUiAttribute -Node $node -Name "class"
            $contentDescription = Get-AndroidUiAttribute -Node $node -Name "content-desc"
            if (
                $className.IndexOf("EditText", [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -or
                $contentDescription -eq "generation-app-job-id-input"
            ) {
                return $true
            }
        }
    } catch {
        return $false
    }

    return $false
}

function Test-AndroidKeyboardLikelyVisible {
    param([string]$UiXml)

    $keyboardKeywordVisible =
        $uiXml.IndexOf("inputmethod", [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -or
        $uiXml.IndexOf("latinime", [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -or
        $uiXml.IndexOf("keyboard", [System.StringComparison]::OrdinalIgnoreCase) -ge 0
    if ($keyboardKeywordVisible) {
        return $true
    }

    $focusedTextInput = Test-AndroidUiHasFocusedTextInput -UiXml $UiXml
    if (-not $focusedTextInput) {
        return $false
    }

    $displayHeight = Get-AndroidDisplayHeight
    $maximumBottom = Get-AndroidUiMaximumBottom -UiXml $UiXml
    return ($displayHeight -gt 0 -and $maximumBottom -gt 0 -and $maximumBottom -lt ($displayHeight * 0.86))
}

function Test-AndroidInputMethodWindowVisible {
    $windows = & $AdbPath @(
        "-s",
        $EmulatorSerial,
        "shell",
        "dumpsys",
        "window",
        "windows"
    )
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        return $false
    }

    $windowText = ($windows -join "`n")
    $inputMethodIndex = $windowText.IndexOf("InputMethod", [System.StringComparison]::OrdinalIgnoreCase)
    if ($inputMethodIndex -lt 0) {
        return $false
    }

    $sliceLength = [Math]::Min(3500, $windowText.Length - $inputMethodIndex)
    $inputMethodWindowText = $windowText.Substring($inputMethodIndex, $sliceLength)
    return (
        $inputMethodWindowText.IndexOf("mViewVisibility=0x0", [System.StringComparison]::OrdinalIgnoreCase) -ge 0 -and
        $inputMethodWindowText.IndexOf("isVisible=true", [System.StringComparison]::OrdinalIgnoreCase) -ge 0
    )
}

function Invoke-AndroidTapContentDescriptionUntil {
    param(
        [string]$ContentDescription,
        [string]$TargetContentDescription,
        [int]$MaxTapAttempts = 3
    )

    $lastFailure = "Android UI content description not found: $TargetContentDescription"
    for ($attempt = 1; $attempt -le $MaxTapAttempts; $attempt += 1) {
        $uiXml = Get-AndroidUiDumpXml -MaxAttempts 2
        if (Test-AndroidUiHasContentDescription `
                -UiXml $uiXml `
                -ContentDescription $TargetContentDescription) {
            return $uiXml
        }

        if (Test-AndroidUiHasContentDescription `
                -UiXml $uiXml `
                -ContentDescription $ContentDescription) {
            $center = Get-AndroidUiCenterByContentDescription `
                -UiXml $uiXml `
                -ContentDescription $ContentDescription
            Invoke-AndroidTapCenter -Name "Tap Android UI '$ContentDescription'" -Center $center
        }

        try {
            return Wait-AndroidUiContentDescription `
                -ContentDescription $TargetContentDescription `
                -MaxAttempts 6
        } catch {
            $lastFailure = $_.Exception.Message
        }

        if (-not (Test-AndroidUiHasContentDescription `
                    -UiXml $uiXml `
                    -ContentDescription $ContentDescription)) {
            Start-Sleep -Milliseconds 600
        }
    }

    throw $lastFailure
}

function Dismiss-AndroidKeyboardIfVisible {
    $uiXml = Get-AndroidUiDumpXml -MaxAttempts 2
    if (
        -not (Test-AndroidKeyboardLikelyVisible -UiXml $uiXml) -and
        -not (Test-AndroidInputMethodWindowVisible)
    ) {
        return
    }

    Invoke-AdbStep -Name "Dismiss Android keyboard with Escape" -ArgumentList @(
        "-s",
        $EmulatorSerial,
        "shell",
        "input",
        "keyevent",
        "111"
    )
    Start-Sleep -Milliseconds 500

    $uiXml = Get-AndroidUiDumpXml -MaxAttempts 2
    if ((Test-AndroidKeyboardLikelyVisible -UiXml $uiXml) -or (Test-AndroidInputMethodWindowVisible)) {
        Invoke-AdbStep -Name "Dismiss Android keyboard with Enter" -ArgumentList @(
            "-s",
            $EmulatorSerial,
            "shell",
            "input",
            "keyevent",
            "66"
        )
        Start-Sleep -Milliseconds 500
    }

    $uiXml = Get-AndroidUiDumpXml -MaxAttempts 2
    if ((Test-AndroidKeyboardLikelyVisible -UiXml $uiXml) -or (Test-AndroidInputMethodWindowVisible)) {
        $displayWidth = Get-AndroidDisplayWidth
        $displayHeight = Get-AndroidDisplayHeight
        if ($displayWidth -gt 0 -and $displayHeight -gt 0) {
            Invoke-AdbStep -Name "Tap Android keyboard action key" -ArgumentList @(
                "-s",
                $EmulatorSerial,
                "shell",
                "input",
                "tap",
                [string][int]($displayWidth * 0.92),
                [string][int]($displayHeight * 0.9)
            )
            Start-Sleep -Milliseconds 500
        }
    }

    $uiXml = Get-AndroidUiDumpXml -MaxAttempts 2
    if ((Test-AndroidKeyboardLikelyVisible -UiXml $uiXml) -or (Test-AndroidInputMethodWindowVisible)) {
        try {
            $heroCenter = Get-AndroidUiCenterByContentDescription `
                -UiXml $uiXml `
                -ContentDescription "generation-studio-hero"
            Invoke-AndroidTapCenter `
                -Name "Tap generation studio hero to clear input focus" `
                -Center $heroCenter
        } catch {
            Invoke-AdbStep -Name "Tap Android UI top area to clear input focus" -ArgumentList @(
                "-s",
                $EmulatorSerial,
                "shell",
                "input",
                "tap",
                "540",
                "220"
            )
        }
        Start-Sleep -Milliseconds 500
    }

    if (Test-AndroidInputMethodWindowVisible) {
        Invoke-AdbStep -Name "Dismiss Android keyboard with Back" -ArgumentList @(
            "-s",
            $EmulatorSerial,
            "shell",
            "input",
            "keyevent",
            "4"
        )
        Start-Sleep -Milliseconds 500
    }

    Wait-AndroidUiContentDescription `
        -ContentDescription "generation-app-job-id-input" `
        -MaxAttempts 4 | Out-Null
}

function Dismiss-AndroidKeyboardBestEffort {
    $uiXml = Get-AndroidUiDumpXml -MaxAttempts 2
    if (
        -not (Test-AndroidKeyboardLikelyVisible -UiXml $uiXml) -and
        -not (Test-AndroidInputMethodWindowVisible)
    ) {
        return
    }

    Invoke-AdbStep -Name "Dismiss Android keyboard with Escape" -ArgumentList @(
        "-s",
        $EmulatorSerial,
        "shell",
        "input",
        "keyevent",
        "111"
    )
    Start-Sleep -Milliseconds 400

    if (Test-AndroidInputMethodWindowVisible) {
        Invoke-AdbStep -Name "Dismiss Android keyboard with Back" -ArgumentList @(
            "-s",
            $EmulatorSerial,
            "shell",
            "input",
            "keyevent",
            "4"
        )
        Start-Sleep -Milliseconds 400
    }
}

function Find-AndroidUiWithContentDescription {
    param(
        [string]$ContentDescription,
        [int]$MaxScrolls = 6
    )

    for ($attempt = 0; $attempt -le $MaxScrolls; $attempt += 1) {
        $uiXml = Get-AndroidUiDumpXml
        try {
            Get-AndroidUiCenterByContentDescription `
                -UiXml $uiXml `
                -ContentDescription $ContentDescription | Out-Null
            return $uiXml
        } catch {
            if ($attempt -eq $MaxScrolls) {
                throw
            }
            Invoke-AndroidSwipeUp
            Start-Sleep -Milliseconds 600
        }
    }
}

function Find-AndroidUiWithTextFragment {
    param(
        [string]$TextFragment,
        [int]$MaxScrolls = 6
    )

    for ($attempt = 0; $attempt -le $MaxScrolls; $attempt += 1) {
        $uiXml = Get-AndroidUiDumpXml
        try {
            Get-AndroidUiCenterByTextFragment `
                -UiXml $uiXml `
                -TextFragment $TextFragment | Out-Null
            return $uiXml
        } catch {
            if ($attempt -eq $MaxScrolls) {
                throw
            }
            Invoke-AndroidSwipeUp
            Start-Sleep -Milliseconds 600
        }
    }
}

function Invoke-AndroidContractDemoUiAssertion {
    if ($SkipLaunch) {
        throw "Contract demo UI assertion requires Android launch."
    }

    Start-Sleep -Seconds 2
    Wait-AndroidUiContentDescription `
        -ContentDescription "launch-bubble-enter" | Out-Null
    Invoke-AndroidTapContentDescriptionUntil `
        -ContentDescription "launch-bubble-enter" `
        -TargetContentDescription "gamer-tab-generate" | Out-Null
    Invoke-AndroidTapContentDescription -ContentDescription "gamer-tab-generate"
    Start-Sleep -Seconds 3

    $snapshots = New-Object System.Collections.Generic.List[string]
    Invoke-AndroidScrollToTop
    $snapshots.Add((Get-AndroidUiDumpXml))
    $snapshots.Add(
        (Find-AndroidUiWithContentDescription `
            -ContentDescription "generation-public-api-boundary-notice" `
            -MaxScrolls 2)
    )
    $snapshots.Add(
        (Find-AndroidUiWithContentDescription `
            -ContentDescription "generation-contract-demo-no-live-worker" `
            -MaxScrolls 6)
    )
    $snapshots.Add(
        (Find-AndroidUiWithContentDescription `
            -ContentDescription "generation-contract-demo-notice" `
            -MaxScrolls 3)
    )

    $candidateUiXml = Find-AndroidUiWithContentDescription `
        -ContentDescription "generation-candidate-select-artifact-1" `
        -MaxScrolls 6
    $snapshots.Add($candidateUiXml)
    $candidateCenter = Get-AndroidUiCenterByContentDescription `
        -UiXml $candidateUiXml `
        -ContentDescription "generation-candidate-select-artifact-1"
    Invoke-AndroidTapCenter -Name "Select contract demo candidate" -Center $candidateCenter
    Start-Sleep -Milliseconds 800
    Dismiss-AndroidKeyboardBestEffort

    $buttonUiXml = Find-AndroidUiWithContentDescription `
        -ContentDescription "generation-review-accept-button" `
        -MaxScrolls 4
    $snapshots.Add($buttonUiXml)
    $packageButtonUiXml = Find-AndroidUiWithContentDescription `
        -ContentDescription "generation-package-download-button" `
        -MaxScrolls 6
    $snapshots.Add($packageButtonUiXml)

    $combinedUiXml = Join-AndroidUiSnapshotXml -Snapshots $snapshots.ToArray()
    return Assert-ContractDemoAndroidUiState -UiXml $combinedUiXml
}

if ($FantasyPetRuleRoot.Trim().Length -eq 0) {
    $FantasyPetRuleRoot = Resolve-DefaultFantasyPetRuleRoot
}
if ($RunRoot.Trim().Length -eq 0) {
    $RunRoot = Join-Path $env:TEMP "fantasy-pet-android-ui"
}
if ($ScreenshotDirectory.Trim().Length -eq 0) {
    $ScreenshotDirectory = Join-Path $RunRoot "screenshots"
}

$FantasyPetRuleRoot = (Resolve-Path -LiteralPath $FantasyPetRuleRoot).Path
$androidRoot = Resolve-AndroidRoot
$runDirectory = Join-Path $RunRoot $AppJobId
$localScreenshotPath = ""
$screenshotLikelyBlank = $false
$contractDemoUiAssertion = $null

if (-not (Test-Path -LiteralPath $GradleWrapper)) {
    throw "Gradle wrapper not found: $GradleWrapper"
}
if (-not (Test-Path -LiteralPath (Join-Path $FantasyPetRuleRoot "tools\run_server_job_lifecycle_demo.py"))) {
    throw "fantasy-pet-rule lifecycle demo script not found under $FantasyPetRuleRoot"
}

if (-not $SkipSeed) {
    Reset-TempDemoRunDirectory -Path $runDirectory
    Invoke-ExternalStep `
        -Name "Seed public fantasy pet demo job" `
        -FilePath "uv" `
        -ArgumentList @(
            "run",
            "--with-requirements",
            "requirements-server.txt",
            "python",
            "tools\run_server_job_lifecycle_demo.py",
            "--run-dir",
            $runDirectory,
            "--app-job-id",
            $AppJobId,
            "--run-id",
            $AppJobId,
            "--description",
            $Description,
            "--body-shape",
            $BodyShape
        ) `
        -WorkingDirectory $FantasyPetRuleRoot
}

$publicApiProcess = $null
$hostPublicApiBaseUrl = "http://127.0.0.1:$PublicApiPort"
try {
    if ($StartPublicApi) {
        if (-not (Test-Path -LiteralPath $RunRoot)) {
            New-Item -ItemType Directory -Path $RunRoot | Out-Null
        }
        $publicApiOut = Join-Path $RunRoot "app-server.out.log"
        $publicApiErr = Join-Path $RunRoot "app-server.err.log"
        Write-Host ""
        Write-Host "== Start public fantasy pet API"
        $publicApiProcess = Start-Process `
            -FilePath "uv" `
            -ArgumentList @(
                "run",
                "--with-requirements",
                "requirements-server.txt",
                "python",
                "tools\app_server.py",
                "--run-root",
                $RunRoot,
                "--host",
                "127.0.0.1",
                "--port",
                [string]$PublicApiPort
            ) `
            -WorkingDirectory $FantasyPetRuleRoot `
            -RedirectStandardOutput $publicApiOut `
            -RedirectStandardError $publicApiErr `
            -WindowStyle Hidden `
            -PassThru
        Wait-PublicApiContract -BaseUrl $hostPublicApiBaseUrl
    } else {
        Write-Host ""
        Write-Host "== Public fantasy pet API"
        Write-Host "Start it separately at $hostPublicApiBaseUrl or rerun with -StartPublicApi."
    }

    if (-not $SkipInstall) {
        $oldFantasyPetApiBaseUrl = $env:FANTASY_PET_API_BASE_URL
        $oldCommunityApiBaseUrl = $env:COMMUNITY_API_BASE_URL
        try {
            $env:FANTASY_PET_API_BASE_URL = $FantasyPetApiBaseUrl
            $env:COMMUNITY_API_BASE_URL = $CommunityApiBaseUrl
            Invoke-ExternalStep `
                -Name "Install Android debug build" `
                -FilePath $GradleWrapper `
                -ArgumentList @(
                    "-p",
                    $androidRoot,
                    "installDebug",
                    "--console=plain",
                    "--rerun-tasks"
                ) `
                -WorkingDirectory (Split-Path -Parent $PSScriptRoot)
        } finally {
            $env:FANTASY_PET_API_BASE_URL = $oldFantasyPetApiBaseUrl
            $env:COMMUNITY_API_BASE_URL = $oldCommunityApiBaseUrl
        }
    }

    if (-not $SkipLaunch) {
        Invoke-AdbStep -Name "Check emulator state" -ArgumentList @(
            "-s",
            $EmulatorSerial,
            "get-state"
        )

        if (-not $SkipClear) {
            Invoke-AdbStep -Name "Clear Android app state" -ArgumentList @(
                "-s",
                $EmulatorSerial,
                "shell",
                "pm",
                "clear",
                "com.gamer.community"
            )
        }

        if ($AssertContractDemoUi) {
            Set-AndroidGenerationJobPreference -AppJobId $AppJobId
        }

        Invoke-AdbStep -Name "Launch Android app" -ArgumentList @(
            "-s",
            $EmulatorSerial,
            "shell",
            "am",
            "start",
            "-n",
            "com.gamer.community/.MainActivity"
        )

        if ($CaptureScreenshot) {
            $localScreenshotPath = Invoke-AndroidScreenshotCapture `
                -OutputDirectory $ScreenshotDirectory `
                -OutputFileName "fantasy-pet-android-ui-smoke-$($AppJobId).png"
            $screenshotLikelyBlank = Test-ScreenshotLikelyBlank -Path $localScreenshotPath
            if ($screenshotLikelyBlank) {
                Write-Warning "Screenshot is mostly black. The emulator display/capture layer may be stale even when the UI tree is present. Restart the emulator before visual QA."
            }
        }

        if ($AssertContractDemoUi) {
            $contractDemoUiAssertion = Invoke-AndroidContractDemoUiAssertion
        }
    } else {
        Write-Host ""
        Write-Host "== Android launch"
        Write-Host "Skipped. Rerun without -SkipLaunch to call adb."
        if ($CaptureScreenshot) {
            Write-Host "Screenshot skipped because Android launch was skipped."
        }
    }

    [pscustomobject]@{
        schema = "gamer.fantasy-pet-android-ui-smoke-launch.v1"
        status = "ready-for-manual-ui-review"
        appJobId = $AppJobId
        runRoot = $RunRoot
        fantasyPetRuleRoot = $FantasyPetRuleRoot
        publicApiStarted = [bool]$StartPublicApi
        publicApiProcessId = if ($publicApiProcess) { $publicApiProcess.Id } else { $null }
        publicApiStoppedOnExit = [bool]($StopPublicApiOnExit -and $publicApiProcess)
        hostPublicApiBaseUrl = $hostPublicApiBaseUrl
        androidFantasyPetApiBaseUrl = $FantasyPetApiBaseUrl
        androidCommunityApiBaseUrl = $CommunityApiBaseUrl
        emulatorSerial = $EmulatorSerial
        androidAppLaunched = -not [bool]$SkipLaunch
        screenshotCaptured = $localScreenshotPath.Trim().Length -gt 0
        screenshotLikelyBlank = $screenshotLikelyBlank
        screenshotPath = $localScreenshotPath
        contractDemoUiAsserted = [bool]$AssertContractDemoUi
        contractDemoUiAssertion = $contractDemoUiAssertion
        nextManualSteps = @(
            "Tap the launch bubble if you did not use -AssertContractDemoUi.",
            "Tap the Generate bottom tab if the app opens on the community feed.",
            "Enter $AppJobId as the App job id and tap Poll job.",
            "Confirm the contract-demo warning and no-live-worker message appear for $AppJobId.",
            "Remember that the demo candidate is a seeded placeholder, not a real generated desktop pet image.",
            "Confirm Accept and Download pet.zip remain disabled for the demo job.",
            "Use a real non-demo generation job when the worker stack is available."
        )
    } | ConvertTo-Json -Depth 4
} finally {
    if ($StopPublicApiOnExit) {
        Stop-PublicFantasyPetApi `
            -Process $publicApiProcess `
            -Port $PublicApiPort `
            -RunRoot $RunRoot
    }
}
