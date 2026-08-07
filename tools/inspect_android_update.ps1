param(
    [Parameter(Mandatory = $true)]
    [string] $ApkPath,
    [string] $Serial,
    [switch] $Install
)

$ErrorActionPreference = "Stop"

$resolvedApk = (Resolve-Path -LiteralPath $ApkPath).Path
$androidSdk = $env:ANDROID_HOME
if ([string]::IsNullOrWhiteSpace($androidSdk)) {
    throw "ANDROID_HOME is not set."
}

$adb = Join-Path $androidSdk "platform-tools\adb.exe"
$buildTools = Get-ChildItem (Join-Path $androidSdk "build-tools") -Directory |
    Sort-Object Name -Descending |
    Select-Object -First 1
if (-not $buildTools) {
    throw "Android SDK build-tools were not found."
}
$aapt = Join-Path $buildTools.FullName "aapt.exe"
$apksigner = Join-Path $buildTools.FullName "apksigner.bat"

foreach ($tool in @($adb, $aapt, $apksigner)) {
    if (-not (Test-Path -LiteralPath $tool)) {
        throw "Required Android tool not found: $tool"
    }
}

function Get-CertificateDigest([string] $Path) {
    $output = & $apksigner verify --print-certs $Path 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "APK signature verification failed for $Path`n$($output -join [Environment]::NewLine)"
    }
    $match = [regex]::Match(($output -join "`n"), "certificate SHA-256 digest:\s*([0-9a-fA-F]+)")
    if (-not $match.Success) {
        throw "Could not read the signing-certificate digest from $Path"
    }
    return $match.Groups[1].Value.ToLowerInvariant()
}

$badging = & $aapt dump badging $resolvedApk
if ($LASTEXITCODE -ne 0) {
    throw "Could not inspect APK metadata."
}
$packageMatch = [regex]::Match(($badging -join "`n"), "package: name='([^']+)' versionCode='([^']+)' versionName='([^']*)'")
if (-not $packageMatch.Success) {
    throw "Could not read the APK package identity."
}

$packageName = $packageMatch.Groups[1].Value
$candidateVersionCode = [long] $packageMatch.Groups[2].Value
$candidateVersionName = $packageMatch.Groups[3].Value
$candidateDigest = Get-CertificateDigest $resolvedApk

$connectedDevices = & $adb devices |
    Select-String "\tdevice$" |
    ForEach-Object { ($_.Line -split "\t")[0] }
if ([string]::IsNullOrWhiteSpace($Serial)) {
    if ($connectedDevices.Count -ne 1) {
        throw "Connect exactly one authorized Android device or pass -Serial. Found $($connectedDevices.Count)."
    }
    $Serial = $connectedDevices[0]
}
elseif ($Serial -notin $connectedDevices) {
    throw "Authorized device '$Serial' was not found."
}
$adbTargetArgs = @("-s", $Serial)

$deviceName = (& $adb @adbTargetArgs shell getprop ro.product.manufacturer).Trim() + " " +
    (& $adb @adbTargetArgs shell getprop ro.product.model).Trim()
$androidVersion = (& $adb @adbTargetArgs shell getprop ro.build.version.release).Trim()
$deviceSdk = (& $adb @adbTargetArgs shell getprop ro.build.version.sdk).Trim()
$installedPathLine = & $adb @adbTargetArgs shell pm path $packageName | Select-Object -First 1

Write-Host "Device: $deviceName (Android $androidVersion, API $deviceSdk, serial $Serial)"
Write-Host "Candidate: $candidateVersionName ($candidateVersionCode)"
Write-Host "Package: $packageName"
Write-Host "Candidate certificate: $candidateDigest"

if ($installedPathLine -match "^package:(.+)$") {
    $devicePackagePath = $matches[1].Trim()
    $packageDetails = & $adb @adbTargetArgs shell dumpsys package $packageName
    $installedVersionMatch = [regex]::Match(($packageDetails -join "`n"), "versionCode=(\d+)")
    $installedVersionCode = if ($installedVersionMatch.Success) { [long] $installedVersionMatch.Groups[1].Value } else { $null }

    $temporaryDirectory = Join-Path ([IO.Path]::GetTempPath()) ("calorie-quick-update-" + [Guid]::NewGuid())
    New-Item -ItemType Directory -Path $temporaryDirectory | Out-Null
    try {
        $installedApk = Join-Path $temporaryDirectory "installed.apk"
        & $adb @adbTargetArgs pull $devicePackagePath $installedApk | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Could not copy the installed APK for certificate comparison."
        }
        $installedDigest = Get-CertificateDigest $installedApk
        Write-Host "Installed version code: $installedVersionCode"
        Write-Host "Installed certificate: $installedDigest"

        if ($installedDigest -ne $candidateDigest) {
            Write-Error "BLOCKED: The installed app and candidate use different signing certificates. Back up app data before planning a signing migration; do not uninstall it."
            exit 2
        }
        if ($null -ne $installedVersionCode -and $candidateVersionCode -lt $installedVersionCode) {
            Write-Error "BLOCKED: The candidate version code is lower than the installed version. Build a newer version instead of downgrading."
            exit 3
        }
        Write-Host "Compatibility: signing identity and version direction are valid."
    }
    finally {
        Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force -ErrorAction SilentlyContinue
    }
}
else {
    Write-Host "Installed app: not found; this will be a fresh installation."
}

if ($Install) {
    Write-Host "Installing with data-preserving replacement enabled..."
    & $adb @adbTargetArgs install -r $resolvedApk
    if ($LASTEXITCODE -ne 0) {
        throw "ADB installation failed. The package-manager error above is the actionable failure reason."
    }
}
else {
    Write-Host "Inspection only. Re-run with -Install after reviewing the result."
}
