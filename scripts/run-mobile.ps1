# Android energy runs through ebserver (userenergy-main/.../ebserver/src/crypto.js).
# -Profile smoke|full|keysizes: the batched protocol of the desktop/Pi runs (crypto.batched.*.json):
#   one process per case, idle window, warm-up (5 s on 4 KiB + 5 s at the size), 5 batches of ~3 s.
#   full = 11 algorithms x 2^11..2^20 B; keysizes = RSA 1024-4096, ElGamal 768-4096, AES/GCM 128-256 at 2/16/128/1024 KiB.
# -Profile legacy-smoke|legacy-full: the old protocol (crypto.config.json / crypto.full.json).
# -BuildType benchmark (default): non-debuggable build for measurements; debug: the debuggable build.
# The phone must be unplugged (Wi-Fi ADB) for formal energy runs.
param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [ValidateSet('smoke','full','keysizes','legacy-smoke','legacy-full')][string]$Profile='smoke',
    [ValidateSet('benchmark','debug')][string]$BuildType='benchmark',
    [switch]$SkipBuild
)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    $cap=$BuildType.Substring(0,1).ToUpper()+$BuildType.Substring(1)
    if (-not $SkipBuild) {
        & .\gradlew.bat "-PtestBuildType=$BuildType" ":app:assemble$cap" ":app:assemble${cap}AndroidTest"
        if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
    }
    & adb -s $Serial install -r "app/build/outputs/apk/$BuildType/app-$BuildType.apk"
    if ($LASTEXITCODE -ne 0) { throw 'Main APK installation failed' }
    & adb -s $Serial install -r "app/build/outputs/apk/androidTest/$BuildType/app-$BuildType-androidTest.apk"
    if ($LASTEXITCODE -ne 0) { throw 'Test APK installation failed' }
    $serverDir=Join-Path $projectRoot 'userenergy-main/userenergy-main/ebserver'
    $config=switch ($Profile) {
        'smoke' {'crypto.batched.smoke.json'}; 'full' {'crypto.batched.full.json'}; 'keysizes' {'crypto.batched.keysizes.json'}
        'legacy-smoke' {'crypto.config.json'}; 'legacy-full' {'crypto.full.json'}
    }
    & node (Join-Path $serverDir 'src/crypto.js') (Join-Path $serverDir $config) "--device=$Serial"
    if ($LASTEXITCODE -ne 0) { throw 'Experiment failed; inspect results/ebserver failure.txt' }
} finally { Pop-Location }
