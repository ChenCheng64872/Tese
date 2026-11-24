# Android energy runs through ebserver (userenergy-main/.../ebserver/src/crypto.js).
# -Profile smoke|full|keysizes: the batched protocol of the desktop/Pi runs (crypto.batched.*.json):
#   one process per case, idle window, warm-up (5 s on 4 KiB + 5 s at the size), 5 batches of ~3 s.
#   full = 11 algorithms x 2^11..2^20 B; keysizes = RSA 1024-4096, ElGamal 768-4096, AES/GCM 128-256 at 2/16/128/1024 KiB.
# -Profile c-full|c-keysizes: the same with the C baseline inside the app (JNI; full adds the "_raw" cipher-only cases).
#   Builds with -PnativeBench; needs c-baseline/fetch-third-party.sh and c-baseline/build-android.sh first (Linux/WSL + NDK r30).
# -Profile legacy-smoke|legacy-full: the old protocol (crypto.config.json / crypto.full.json).
# -BuildType release (default): the release build, used for measurements; debug: the debuggable build.
# The phone must be unplugged (Wi-Fi ADB) for formal energy runs.
param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [ValidateSet('smoke','full','keysizes','c-full','c-keysizes','legacy-smoke','legacy-full')][string]$Profile='smoke',
    [ValidateSet('release','debug')][string]$BuildType='release',
    [switch]$SkipBuild
)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    $cap=$BuildType.Substring(0,1).ToUpper()+$BuildType.Substring(1)
    if (-not $SkipBuild) {
        $extra=@(); if ($Profile -like 'c-*') { $extra+='-PnativeBench' }
        & .\gradlew.bat "-PtestBuildType=$BuildType" @extra ":app:assemble$cap" ":app:assemble${cap}AndroidTest"
        if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
    }
    & adb -s $Serial install -r "app/build/outputs/apk/$BuildType/app-$BuildType.apk"
    if ($LASTEXITCODE -ne 0) { throw 'Main APK installation failed' }
    & adb -s $Serial install -r "app/build/outputs/apk/androidTest/$BuildType/app-$BuildType-androidTest.apk"
    if ($LASTEXITCODE -ne 0) { throw 'Test APK installation failed' }
    $serverDir=Join-Path $projectRoot 'userenergy-main/userenergy-main/ebserver'
    $config=switch ($Profile) {
        'smoke' {'crypto.batched.smoke.json'}; 'full' {'crypto.batched.full.json'}; 'keysizes' {'crypto.batched.keysizes.json'}
        'c-full' {'crypto.batched.c-full.json'}; 'c-keysizes' {'crypto.batched.c-keysizes.json'}
        'legacy-smoke' {'crypto.config.json'}; 'legacy-full' {'crypto.full.json'}
    }
    & node (Join-Path $serverDir 'src/crypto.js') (Join-Path $serverDir $config) "--device=$Serial"
    if ($LASTEXITCODE -ne 0) { throw 'Experiment failed; inspect results/ebserver failure.txt' }
} finally { Pop-Location }
