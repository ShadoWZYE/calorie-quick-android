[CmdletBinding()]
param(
    [string]$AvdName = "CalorieQuick_API_37",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Resolve-ToolchainPath {
    param(
        [string]$EnvironmentName,
        [string]$Fallback
    )

    $value = [Environment]::GetEnvironmentVariable($EnvironmentName, "Process")
    if (-not $value) {
        $value = [Environment]::GetEnvironmentVariable($EnvironmentName, "User")
    }
    if (-not $value) {
        $value = $Fallback
    }
    return $value
}

function Get-RunningEmulatorSerial {
    param(
        [string]$AdbPath,
        [string]$ExpectedAvdName
    )

    $lines = & $AdbPath devices 2>$null
    foreach ($line in $lines) {
        if ($line -match "^(emulator-\d+)\s+device$") {
            $candidate = $Matches[1]
            $runningName = (& $AdbPath -s $candidate emu avd name 2>$null | Select-Object -First 1).Trim()
            if ($runningName -eq $ExpectedAvdName) {
                return $candidate
            }
        }
    }
    return $null
}

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$sdkRoot = Resolve-ToolchainPath `
    -EnvironmentName "ANDROID_HOME" `
    -Fallback (Join-Path $env:LOCALAPPDATA "Android\sdk")
$javaHome = Resolve-ToolchainPath `
    -EnvironmentName "JAVA_HOME" `
    -Fallback (Join-Path $env:LOCALAPPDATA "CalorieQuickToolchain\jdk\jdk-17.0.20+8")

$adb = Join-Path $sdkRoot "platform-tools\adb.exe"
$emulator = Join-Path $sdkRoot "emulator\emulator.exe"
$gradleWrapper = Join-Path $repoRoot "gradlew.bat"
$apkOutputDirectory = Join-Path $repoRoot "app\build\outputs\apk\debug"

foreach ($requiredFile in @($adb, $emulator, $gradleWrapper, (Join-Path $javaHome "bin\java.exe"))) {
    if (-not (Test-Path -LiteralPath $requiredFile)) {
        throw "Required tool was not found: $requiredFile"
    }
}

$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot
$env:JAVA_HOME = $javaHome
$env:Path = "$(Join-Path $javaHome 'bin');$(Join-Path $sdkRoot 'platform-tools');$env:Path"

Set-Location $repoRoot

Write-Step "Checking Android emulator acceleration"
$accelOutput = & (Join-Path $sdkRoot "emulator\emulator-check.exe") accel 2>&1
if ($LASTEXITCODE -ne 0) {
    $accelOutput | Write-Host
    throw "Android emulator acceleration is unavailable."
}
Write-Host "AEHD acceleration is ready." -ForegroundColor Green

$serial = Get-RunningEmulatorSerial -AdbPath $adb -ExpectedAvdName $AvdName
if (-not $serial) {
    Write-Step "Starting $AvdName"
    Start-Process -FilePath $emulator -ArgumentList @(
        "-avd", $AvdName,
        "-netdelay", "none",
        "-netspeed", "full"
    ) | Out-Null
} else {
    Write-Step "Reusing running emulator $serial"
}

if (-not $SkipBuild) {
    Write-Step "Building the latest debug APK"
    & $gradleWrapper assembleDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE."
    }
}

$apkFile = Get-ChildItem -LiteralPath $apkOutputDirectory -Filter "CalorieQuick-*-debug.apk" -File `
    | Sort-Object LastWriteTimeUtc -Descending `
    | Select-Object -First 1
if (-not $apkFile) {
    throw "A versioned debug APK was not found in $apkOutputDirectory. Run without -SkipBuild first."
}
$apk = $apkFile.FullName
Write-Host "Using $($apkFile.Name)" -ForegroundColor Green

Write-Step "Waiting for Android to finish booting"
$deadline = (Get-Date).AddMinutes(3)
do {
    $serial = Get-RunningEmulatorSerial -AdbPath $adb -ExpectedAvdName $AvdName
    if ($serial) {
        $bootCompleted = (& $adb -s $serial shell getprop sys.boot_completed 2>$null).Trim()
        if ($bootCompleted -eq "1") {
            break
        }
    }
    Write-Host "." -NoNewline
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)
Write-Host ""

if (-not $serial -or $bootCompleted -ne "1") {
    throw "The emulator did not finish booting within three minutes."
}

Write-Step "Installing Calorie Quick"
& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) {
    throw "APK installation failed with exit code $LASTEXITCODE."
}

Write-Step "Launching the app"
& $adb -s $serial shell am force-stop com.shadow.calorietracker
& $adb -s $serial shell am start -n com.shadow.calorietracker/.MainActivity
if ($LASTEXITCODE -ne 0) {
    throw "The app could not be launched."
}

Write-Host ""
Write-Host "Calorie Quick is ready for testing on $serial." -ForegroundColor Green
Write-Host "Leave the emulator open while testing. Close its window when finished."
