param(
    [Parameter(Mandatory)][string]$PowerJoular,
    [string]$ProjectRoot = $PSScriptRoot,
    [string]$OutputDirectory = (Join-Path $PSScriptRoot 'energy-results')
)
$ErrorActionPreference = 'Stop'
$PowerJoular = (Resolve-Path -LiteralPath $PowerJoular).Path
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$benchmark = Join-Path $ProjectRoot 'desktop/build/install/crypto-desktop/bin/crypto-desktop.bat'
if (!(Test-Path -LiteralPath $benchmark)) {
    throw 'Build the benchmark first with gradlew.bat -p desktop installDist.'
}
$run = Join-Path $OutputDirectory ('run-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $run -Force | Out-Null
$run = (Resolve-Path -LiteralPath $run).Path
$raw = Join-Path $run 'power.csv'
$algorithms = @('aes','aesgcm','chacha20','rsa','elgamal','ascon','elephant','giftcofb','grain128','xoodyak')
$logger = Start-Process -FilePath $PowerJoular -ArgumentList @('-f', ('"{0}"' -f $raw)) -WindowStyle Hidden -PassThru
try {
    Start-Sleep -Seconds 3
    foreach ($exponent in 11..20) {
        foreach ($algorithm in $algorithms) {
            $timings = Join-Path $run "$algorithm-$exponent.csv"
            & $benchmark $algorithm $exponent 5 1 $timings 3000
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
$rows = foreach ($exponent in 11..20) {
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
            estimated_cpu_package_j = if ($valid) { [Math]::Round($energy, 6) }
            estimated_cpu_j_per_pair = if ($valid) { [Math]::Round($energy / [int]$timing.inner_iterations, 9) }
            energy_status = if ($valid) { 'OK' } else { 'INCOMPLETE_COVERAGE' }
        }
    }
}
}
if ($rows.Count -ne 500) { throw "Expected 500 Windows batches" }
$rows | Sort-Object size_kib, algorithm, repetition | Export-Csv -LiteralPath (Join-Path $run 'results.csv') -NoTypeInformation
Write-Output "Results and raw evidence: $run"
