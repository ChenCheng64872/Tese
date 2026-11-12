# Cryptographic benchmark results

- [English benchmark report and charts](Cryptographic-Benchmark-Report-EN.html)
- [Chinese and Portuguese supervisor briefing](导师汇报稿-中文与葡萄牙语.md)
- [Desktop, Android and IoT comparison](Comparacao-Desktop-Android-IoT-PT.md)
- [Batch data](android-energy-gap-fill-final/cryptography-batches-integrated.csv), [grouped results](android-energy-gap-fill-final/cryptography-summary-by-case.csv), and [matched timings](android-energy-gap-fill-final/platform-timing-comparison.csv)

The final dataset contains 500 Windows batches and 470 Android batches. Each included algorithm–size case has five batches. The Android 256–1,024 KiB series covers eight algorithms. Elephant200 and Grain-128AEAD have data at 2–128 KiB. Batch and rerun IDs preserve the join between CSVs. The published exports omit machine-specific paths and device identifiers.

Windows energy represents CPU-package joules per encryption–decryption pair. Android energy is a provisional battery-instrumentation estimate over each sampling window. The IoT appendix documents the different measurement boundaries and the scope of the AES reference check.

## Reproduce the experiment

Run the following from the repository root in PowerShell. Use an Android SDK with ADB available and authorize the test device.

```powershell
.\scripts\run-mobile.ps1 -Serial '<ADB serial>' -Profile smoke
$ebserver = 'userenergy-main/userenergy-main/ebserver'
node "$ebserver/src/crypto.test.js"
node "$ebserver/src/crypto.js" "$ebserver/crypto.android-2-to-128-kib.json" --dry-run
node "$ebserver/src/crypto.js" "$ebserver/crypto.android-256-to-1024-kib.json" --dry-run
```

For sampling on the IV2201, use wireless ADB, disconnect charging and confirm the phone is discharging. Run the selected configuration without `--dry-run`, using `--device=<ADB serial>` to identify the device. The two configurations reproduce the cases in the published dataset. The recovered-energy CSVs record later retries.

On Windows, use PowerJoular 2.0.0-beta-1 and a machine with CPU-package readings available.

```powershell
.\gradlew.bat -p desktop installDist
.\scripts\run-desktop-energy-5.ps1 -PowerJoular '<path to powerjoular.exe>' -ProjectRoot . -OutputDirectory results/windows-five-batch-study
```

The [desktop runner](../scripts/run-desktop-energy-5.ps1) collects the power trace, runs five batches per case, and integrates the power samples over the timing windows. The desktop benchmark records the operation durations using `System.nanoTime()`.
