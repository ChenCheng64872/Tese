# Windows runner, aligned with the Raspberry Pi and macOS runners (same protocol, same harnesses):
#   * every case in its own process: warm-up 5 s on a 4 KiB input + 5 s at the size, then 5 batches of ~3 s
#   * idle baseline at the start and at the end (-IdleSeconds), an idle window before every case
#     (-PreIdleSeconds), algorithm order shuffled per size block (-Seed, recorded in run.json) and
#     a cooldown after every size block (-BlockCooldownSeconds)
#   * energy: PowerJoular (CPU power, 1 s interval averages), integrated over every batch window; dynamic
#     energy subtracts the run idle baseline and the case's own idle window (same columns as the Pi results)
#   (No thermal gate: Windows has no reliable CPU temperature reading. Keep the PC on AC power, close other apps.)
#
# -Impl java (default): the Java harness (desktop/). -Impl c: the C baseline (c-baseline/build/crypto-c.exe).
# -KeySizes: RSA 1024-4096, ElGamal 768-4096, AES/AES-GCM 128/192/256 at 2, 16, 128 and 1024 KiB.
# -Smoke: 2-minute check (3 algorithms, 2 KiB, 2 batches of 1 s, short pauses).
param(
    [Parameter(Mandatory)][string]$PowerJoular,
    [string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent),
    [string]$OutputDirectory = (Join-Path (Split-Path $PSScriptRoot -Parent) 'results'),
    [ValidateSet('java', 'c')][string]$Impl = 'java',
    [switch]$KeySizes,
    [switch]$Smoke,
    [int]$Seed = [int]([DateTimeOffset]::UtcNow.ToUnixTimeSeconds() % 1000000),
    [int]$IdleSeconds = 30,
    [int]$PreIdleSeconds = 5,
    [int]$BlockCooldownSeconds = 30
)
$ErrorActionPreference = 'Stop'
$PowerJoular = (Resolve-Path -LiteralPath $PowerJoular).Path
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
if ($Impl -eq 'java') {
    $benchmark = Join-Path $ProjectRoot 'desktop/build/install/crypto-desktop/bin/crypto-desktop.bat'
    $benchmarkId = Join-Path $ProjectRoot 'desktop/build/install/crypto-desktop/lib/crypto-desktop.jar'
    $buildHint = 'Build the benchmark first with gradlew.bat -p desktop installDist.'
} else {
    $benchmark = Join-Path $ProjectRoot 'c-baseline/build/crypto-c.exe'
    $benchmarkId = $benchmark
    $buildHint = 'Build the C baseline first (see c-baseline/README.md).'
}
if (!(Test-Path -LiteralPath $benchmark)) { throw $buildHint }

$batches = 5; $targetMs = 3000; $warmupMs = 5000; $warmupSmallPower = 12
$base = @('aes','aesgcm','chacha20','rsa','elgamal','ascon','elephant','giftcofb','grain128','xoodyak')
if ($KeySizes) {
    $exponents = @(11, 14, 17, 20)
    $algorithms = @('rsa1024','rsa2048','rsa3072','rsa4096','elgamal768','elgamal1024','elgamal2048','elgamal3072','elgamal4096',
                    'aes128','aes192','aes256','aesgcm128','aesgcm192','aesgcm256')
    if ($Impl -eq 'java') { $algorithms += @('aesgcmbc128','aesgcmbc192','aesgcmbc256') }
} elseif ($Smoke) {
    $exponents = @(11)
    $algorithms = if ($Impl -eq 'java') { @('aes','aesgcm','ascon') } else { @('aes','aesgcm','ascon','ascon_raw') }
    $batches = 2; $targetMs = 1000; $warmupMs = 1000; $IdleSeconds = 5; $PreIdleSeconds = 2; $BlockCooldownSeconds = 5
} else {
    $exponents = 11..20
    if ($Impl -eq 'java') { $algorithms = $base + @('aesgcmbc') }
    else { $algorithms = $base + ($base | ForEach-Object { "${_}_raw" }) }  # C: mirror + raw modes
}

$computer = (Get-CimInstance Win32_ComputerSystem)
$board = 'pc-' + (($computer.Model -replace '[^A-Za-z0-9]', '').ToLower())
$suffix = if ($Impl -eq 'c') { '-c' } else { '' }
$run = Join-Path $OutputDirectory ("$board$suffix/run-" + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path (Join-Path $run 'timings') -Force | Out-Null
$run = (Resolve-Path -LiteralPath $run).Path
$raw = Join-Path $run 'powerjoular.csv'
$events = Join-Path $run 'events.csv'
'epoch_ms,event,detail' | Set-Content -LiteralPath $events -Encoding ascii
function NowMs { [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() }
function Add-Event([string]$name, [string]$detail = '') { "$(NowMs),$name,$detail" | Add-Content -LiteralPath $events -Encoding ascii }
function Write-Log([string]$text) { $line = "[$(Get-Date -Format HH:mm:ss)] $text"; Write-Output $line; $line | Add-Content -LiteralPath (Join-Path $run 'run.log') }
function Get-Shuffled([string[]]$items, [int]$exponent) {
    if ($Seed -lt 0) { return $items }
    $rng = [System.Random]::new($Seed * 1000 + $exponent); $a = [string[]]$items.Clone()
    for ($i = $a.Length - 1; $i -gt 0; $i--) { $j = $rng.Next($i + 1); $t = $a[$i]; $a[$i] = $a[$j]; $a[$j] = $t }
    return $a
}

$cpu = (Get-CimInstance Win32_Processor | Select-Object -First 1)
$os = (Get-CimInstance Win32_OperatingSystem)
$javaVersion = if ($Impl -eq 'java') { (& cmd /c 'java -version 2>&1' | Select-Object -First 1) } else { '' }
[ordered]@{
    device = "$($computer.Manufacturer) $($computer.Model)"; board = $board; cpu = $cpu.Name.Trim()
    cores = $cpu.NumberOfCores; logical_processors = $cpu.NumberOfLogicalProcessors; memory_bytes = $computer.TotalPhysicalMemory
    os = "$($os.Caption) $($os.Version)"; java = "$javaVersion"
    power_plan = ((& powercfg /getactivescheme) -join ' ')
    on_battery = [bool]((Get-CimInstance Win32_Battery -ErrorAction SilentlyContinue | Where-Object { $_.BatteryStatus -eq 1 }))
    tese_commit = ((& git -C $ProjectRoot rev-parse HEAD 2>$null) -join ''); implementation = $Impl
    harness_sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $benchmarkId).Hash.ToLower(); harness_path = $benchmarkId
    config = [ordered]@{ algorithms = $algorithms; exponents = $exponents; batches = $batches; target_batch_ms = $targetMs
        inner_iterations = 1; warmup_ms = $warmupMs; warmup_small_power = $warmupSmallPower; idle_baseline_s = $IdleSeconds
        pre_case_idle_s = $PreIdleSeconds; block_cooldown_s = $BlockCooldownSeconds; thermal_gate = 'none (no reliable sensor on Windows)'
        shuffle = $true; seed = "$Seed" }
    energy_source = 'powerjoular'
    energy_source_detail = 'PowerJoular for Windows, 1 s interval averages of CPU power, held over each interval'
} | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $run 'run.json') -Encoding utf8

$logger = Start-Process -FilePath $PowerJoular -ArgumentList @('-f', ('"{0}"' -f $raw)) -WindowStyle Hidden -PassThru
$failures = 0
try {
    Start-Sleep -Seconds 3
    Write-Log "Run directory: $run"
    Write-Log "PC: $($cpu.Name.Trim()) | implementation: $Impl | seed $Seed"
    Write-Log "Algorithms: $($algorithms -join ' ') | exponents: $($exponents -join ' ') | batches: $batches x $targetMs ms"
    Write-Log "Idle baseline (start): $IdleSeconds s"
    Add-Event idle_start start; Start-Sleep -Seconds $IdleSeconds; Add-Event idle_end start
    for ($b = 0; $b -lt $exponents.Count; $b++) {
        $exponent = $exponents[$b]
        $order = Get-Shuffled $algorithms $exponent
        Write-Log "=== Block 2^$exponent bytes ($([int](1 -shl $exponent) / 1024) KiB): $($order -join ' ') ==="
        foreach ($algorithm in $order) {
            $id = "$algorithm-$exponent"
            Add-Event pre_idle_start $id; Start-Sleep -Seconds $PreIdleSeconds; Add-Event pre_idle_end $id
            $timings = Join-Path $run "timings/$id.csv"
            Add-Event case_start $id
            & $benchmark $algorithm $exponent $batches 1 $timings $targetMs $warmupMs $warmupSmallPower *> (Join-Path $run "timings/$id.log")
            if ($LASTEXITCODE -eq 0) { Add-Event case_end $id; Write-Log "  $algorithm 2^$exponent done" }
            else { Add-Event case_failed $id; Write-Log "  $algorithm 2^$exponent FAILED (see timings/$id.log)"; $failures++ }
        }
        if ($b -lt $exponents.Count - 1) {
            Write-Log "Cooldown $BlockCooldownSeconds s"
            Add-Event cooldown_start $exponent; Start-Sleep -Seconds $BlockCooldownSeconds; Add-Event cooldown_end $exponent
        }
    }
    Write-Log "Idle baseline (end): $IdleSeconds s"
    Add-Event idle_start end; Start-Sleep -Seconds $IdleSeconds; Add-Event idle_end end
    Start-Sleep -Seconds 2
} finally {
    if (!$logger.HasExited) {
        Stop-Process -Id $logger.Id
        $logger.WaitForExit()
    }
}

# ---- energy per batch (same method as scripts/pi/pi_integrate_energy.py for PowerJoular data)
$power = @(Import-Csv -LiteralPath $raw | ForEach-Object {
    $t = 0.0; $w = 0.0
    if ([double]::TryParse($_.Timestamp, [ref]$t) -and [double]::TryParse($_.'CPU Power', [ref]$w)) { [pscustomobject]@{ t = $t; w = $w } }
})
if ($power.Count -lt 2) { throw 'PowerJoular did not collect enough samples.' }
# Each sample is the average power over the interval that ends at its timestamp (held over it). Windows PowerJoular
# timestamps are whole seconds, so a 1 s interval can appear as a 2 s step; longer gaps are not covered.
function Measure-Energy([double]$start, [double]$end) {
    $energy = 0.0; $coverage = 0.0; $samples = 0
    for ($i = 1; $i -lt $power.Count; $i++) {
        $left = $power[$i - 1].t; $right = $power[$i].t
        if ($left -ge $end) { break }
        if ($right -le $start) { continue }
        if ($right - $left -le 0 -or $right - $left -gt 2.0 -or $power[$i].w -lt 0) { continue }
        $overlap = [Math]::Min($end, $right) - [Math]::Max($start, $left)
        if ($overlap -le 0) { continue }
        $coverage += $overlap; $energy += $power[$i].w * $overlap; $samples++
    }
    [pscustomobject]@{ energy = $energy; coverage = $coverage; samples = $samples }
}
$ev = @(Import-Csv -LiteralPath $events)
function Get-Window([string]$startName, [string]$endName, [string]$detail, [double]$skip) {
    $a = $ev | Where-Object { $_.event -eq $startName -and $_.detail -eq $detail } | Select-Object -First 1
    $z = $ev | Where-Object { $_.event -eq $endName -and $_.detail -eq $detail } | Select-Object -First 1
    if ($a -and $z) { @(([double]$a.epoch_ms / 1000 + $skip), ([double]$z.epoch_ms / 1000)) }
}
$idleE = 0.0; $idleC = 0.0
foreach ($d in @('start', 'end')) {
    $w = Get-Window idle_start idle_end $d 2.0
    if ($w) { $m = Measure-Energy $w[0] $w[1]; $idleE += $m.energy; $idleC += $m.coverage }
}
$idleW = if ($idleC -gt 0) { $idleE / $idleC } else { $null }
$rows = foreach ($exponent in $exponents) {
    foreach ($algorithm in $algorithms) {
        $file = Join-Path $run "timings/$algorithm-$exponent.csv"
        if (!(Test-Path -LiteralPath $file)) { continue }
        $w = Get-Window pre_idle_start pre_idle_end "$algorithm-$exponent" 1.0
        $caseIdleW = $null
        if ($w) { $m = Measure-Energy $w[0] $w[1]; if ($m.coverage -gt 0) { $caseIdleW = $m.energy / $m.coverage } }
        foreach ($timing in Import-Csv -LiteralPath $file) {
            $start = [double]$timing.batch_start_epoch_ms / 1000
            $end = [double]$timing.batch_end_epoch_ms / 1000
            $dur = $end - $start
            $pairs = [int]$timing.inner_iterations
            $m = Measure-Energy $start $end
            $valid = $m.coverage -ge ($dur - 0.15) -and $m.coverage -gt 0
            [pscustomobject][ordered]@{
                repetition = [int]$timing.round
                algorithm = $timing.algorithm
                size_kib = [int]$timing.size_bytes / 1024
                size_bytes = $timing.size_bytes
                pairs = $pairs
                enc_mean_ns = $timing.enc_mean_ns
                dec_mean_ns = $timing.dec_mean_ns
                batch_start_epoch_ms = $timing.batch_start_epoch_ms
                batch_end_epoch_ms = $timing.batch_end_epoch_ms
                batch_duration_s = [Math]::Round($dur, 3)
                energy_source = 'powerjoular'
                power_samples = $m.samples
                coverage_s = [Math]::Round($m.coverage, 3)
                core_j = $(if ($valid) { [Math]::Round($m.energy, 6) })
                core_j_per_pair = $(if ($valid) { [Math]::Round($m.energy / $pairs, 9) })
                idle_core_w = $(if ($null -ne $idleW) { [Math]::Round($idleW, 5) })
                core_dynamic_j_per_pair = $(if ($valid -and $null -ne $idleW) { [Math]::Round(($m.energy - $idleW * $dur) / $pairs, 9) })
                case_idle_core_w = $(if ($null -ne $caseIdleW) { [Math]::Round($caseIdleW, 5) })
                core_dynamic_case_j_per_pair = $(if ($valid -and $null -ne $caseIdleW) { [Math]::Round(($m.energy - $caseIdleW * $dur) / $pairs, 9) })
                estimated_cpu_package_j = $(if ($valid) { [Math]::Round($m.energy, 6) })
                estimated_cpu_j_per_pair = $(if ($valid) { [Math]::Round($m.energy / $pairs, 9) })
                energy_status = $(if ($valid) { 'OK' } else { 'INCOMPLETE_COVERAGE' })
            }
        }
    }
}
$expected = $exponents.Count * $algorithms.Count * $batches
if (@($rows).Count -ne $expected) { Write-Log "WARNING: expected $expected batches, got $(@($rows).Count) ($failures failed cases)" }
$rows | Sort-Object size_kib, algorithm, repetition | Export-Csv -LiteralPath (Join-Path $run 'results.csv') -NoTypeInformation
$ok = @($rows | Where-Object { $_.energy_status -eq 'OK' }).Count
Write-Log "$(@($rows).Count) batches, $ok OK. Failures: $failures. Results and raw evidence: $run"
