# -Impl java (default): the Java harness (desktop/). -Impl c: the C baseline (c-baseline/build/crypto-c.exe).
# -KeySizes: key-size experiment (RSA 1024-4096, ElGamal 768-4096, AES 128/192/256) at 2, 16, 128 and 1024 KiB.
# Every case warms up for 5 s on a 4 KiB input and 5 s at the measured size before the 5 batches.
param(
    [Parameter(Mandatory)][string]$PowerJoular,
    [string]$ProjectRoot = $PSScriptRoot,
    [string]$OutputDirectory = (Join-Path $PSScriptRoot 'energy-results'),
    [ValidateSet('java', 'c')][string]$Impl = 'java',
    [switch]$KeySizes
)
$ErrorActionPreference = 'Stop'
$PowerJoular = (Resolve-Path -LiteralPath $PowerJoular).Path
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
if ($Impl -eq 'java') {
    $benchmark = Join-Path $ProjectRoot 'desktop/build/install/crypto-desktop/bin/crypto-desktop.bat'
    $buildHint = 'Build the benchmark first with gradlew.bat -p desktop installDist.'
} else {
    $benchmark = Join-Path $ProjectRoot 'c-baseline/build/crypto-c.exe'
    $buildHint = 'Build the C baseline first (see c-baseline/README.md).'
}
if (!(Test-Path -LiteralPath $benchmark)) { throw $buildHint }
$run = Join-Path $OutputDirectory ("run-$Impl-" + [guid]::NewGuid())
New-Item -ItemType Directory -Path $run -Force | Out-Null
$run = (Resolve-Path -LiteralPath $run).Path
$raw = Join-Path $run 'power.csv'
$warmupMs = 5000
$warmupSmallPower = 12
if ($KeySizes) {
    $exponents = @(11, 14, 17, 20)
    $algorithms = @('rsa1024','rsa2048','rsa3072','rsa4096','elgamal768','elgamal1024','elgamal2048','elgamal3072','elgamal4096',
                    'aes128','aes192','aes256','aesgcm128','aesgcm192','aesgcm256')
    if ($Impl -eq 'java') { $algorithms += @('aesgcmbc128','aesgcmbc192','aesgcmbc256') }
} else {
    $exponents = 11..20
    $base = @('aes','aesgcm','chacha20','rsa','elgamal','ascon','elephant','giftcofb','grain128','xoodyak')
    if ($Impl -eq 'java') { $algorithms = $base + @('aesgcmbc') }
    else { $algorithms = $base + ($base | ForEach-Object { "${_}_raw" }) }  # C: mirror + raw modes
}
$logger = Start-Process -FilePath $PowerJoular -ArgumentList @('-f', ('"{0}"' -f $raw)) -WindowStyle Hidden -PassThru
try {
    Start-Sleep -Seconds 3
    foreach ($exponent in $exponents) {
        foreach ($algorithm in $algorithms) {
            $timings = Join-Path $run "$algorithm-$exponent.csv"
            & $benchmark $algorithm $exponent 5 1 $timings 3000 $warmupMs $warmupSmallPower
            if ($LASTEXITCODE -ne 0) { throw "Benchmark failed: $algorithm, 2^$exponent bytes" }
        }
    }
    Start-Sleep -Seconds 2
} finally {
    if (!$logger.HasExited) {
        Stop-Process -Id $logger.Id
        $logger.WaitForExit()
    }
}

$power = @(Import-Csv -LiteralPath $raw)
if ($power.Count -lt 2) { throw 'PowerJoular did not collect enough samples.' }
$rows = foreach ($exponent in $exponents) {
    foreach ($algorithm in $algorithms) {
        foreach ($timing in Import-Csv -LiteralPath (Join-Path $run "$algorithm-$exponent.csv")) {
        $start = [double]$timing.batch_start_epoch_ms / 1000
        $end = [double]$timing.batch_end_epoch_ms / 1000
        $energy = 0.0
        $coverage = 0.0
        $samples = 0
        for ($index = 1; $index -lt $power.Count; $index++) {
            $left = [double]$power[$index - 1].Timestamp
            $right = [double]$power[$index].Timestamp
            if ($left -ge $end) { break }
            if ($right -le $start) { continue }
            $watts = [double]$power[$index].'CPU Power'
            if ($right - $left -ne 1 -or $watts -le 0) { continue }
            $overlap = [Math]::Min($end, $right) - [Math]::Max($start, $left)
            if ($overlap -le 0) { continue }
            $coverage += $overlap
            $energy += $watts * $overlap
            $samples++
        }
        $valid = $coverage -ge ($end - $start - 0.001)
        [pscustomobject]@{
            repetition = [int]$timing.round
            algorithm = $timing.algorithm
            size_kib = [int]$timing.size_bytes / 1024
            size_bytes = $timing.size_bytes
            pairs = $timing.inner_iterations
            enc_mean_ns = $timing.enc_mean_ns
            dec_mean_ns = $timing.dec_mean_ns
            batch_start_epoch_ms = $timing.batch_start_epoch_ms
            batch_end_epoch_ms = $timing.batch_end_epoch_ms
            batch_duration_s = [Math]::Round($end - $start, 3)
            cpu_samples = $samples
            coverage_s = [Math]::Round($coverage, 3)
            estimated_cpu_package_j = $(if ($valid) { [Math]::Round($energy, 6) })
            estimated_cpu_j_per_pair = $(if ($valid) { [Math]::Round($energy / [int]$timing.inner_iterations, 9) })
            energy_status = $(if ($valid) { 'OK' } else { 'INCOMPLETE_COVERAGE' })
        }
    }
}
}
$expected = $exponents.Count * $algorithms.Count * 5
if ($rows.Count -ne $expected) { throw "Expected $expected Windows batches, got $($rows.Count)" }
$rows | Sort-Object size_kib, algorithm, repetition | Export-Csv -LiteralPath (Join-Path $run 'results.csv') -NoTypeInformation
Write-Output "Results and raw evidence: $run"
