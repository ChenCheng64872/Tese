param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [ValidateSet('smoke','full')][string]$Profile='smoke',
    [switch]$SkipBuild
)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    if (-not $SkipBuild) {
        & .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
        if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
    }
    & adb -s $Serial install -r app/build/outputs/apk/debug/app-debug.apk
    if ($LASTEXITCODE -ne 0) { throw 'Main APK installation failed' }
    & adb -s $Serial install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    if ($LASTEXITCODE -ne 0) { throw 'Test APK installation failed' }
    $serverDir=Join-Path $projectRoot 'userenergy-main/userenergy-main/ebserver'
    $config=if($Profile -eq 'full'){'crypto.full.json'}else{'crypto.config.json'}
    & node (Join-Path $serverDir 'src/crypto.js') (Join-Path $serverDir $config) "--device=$Serial"
    if ($LASTEXITCODE -ne 0) { throw 'Experiment failed; inspect results/ebserver failure.txt' }
} finally { Pop-Location }
