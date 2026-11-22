package com.example.energy
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/**
 * One benchmark case per instrumentation run (a fresh process, like one JVM per case on desktop),
 * with the desktop protocol (DesktopMain.kt) and the same code (CryptoCase):
 *   1. idle window of idle_ms (battery-current baseline for dynamic energy)
 *   2. time-based warm-up: warmup_ms on a 2^warmup_small_power-byte input, then warmup_ms on the measured size
 *   3. `rounds` batches; each repeats inner_iterations encryptions + decryptions until target_ms has passed,
 *      and its energy is measured with EnergyMeter (battery current x voltage, or the energy counter).
 * Arguments (-e): algorithm (names/key sizes as in CryptoCase), contentsize (power of two),
 * rounds, target_ms, warmup_ms, warmup_small_power, idle_ms, inner_iterations, run_id.
 */
@RunWith(AndroidJUnit4::class)
class CryptoBenchmarkInstrumentedTest {
    @Test fun runbenchmark() {
        val a=InstrumentationRegistry.getArguments()
        fun long(name:String, default:Long)=a.getString(name)?.toLong() ?: default
        val algorithm=a.getString("algorithm") ?: "aesgcm"
        val runId=a.getString("run_id") ?: "manual_"+System.currentTimeMillis()
        require(runId.matches(Regex("[A-Za-z0-9_-]+")))
        val p=long("contentsize",10).toInt(); val rounds=long("rounds",5).toInt()
        val inner=long("inner_iterations",1).toInt(); val targetMs=long("target_ms",3000)
        val warmupMs=long("warmup_ms",5000); val smallPower=long("warmup_small_power",12).toInt()
        val idleMs=long("idle_ms",5000)
        require(p in 10..20 && rounds in 1..100 && inner in 1..100000 && targetMs in 0..600000 &&
            warmupMs in 0..600000 && (smallPower == 0 || smallPower in 6..20) && idleMs in 0..600000)
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val external=a.getString("external_energy_probe") == "true"
        val meter=EnergyMeter(context)

        val case=CryptoCase(algorithm)
        val plain="A".repeat(1 shl p)
        val (encrypt, decrypt) = case.build(plain)

        // 1. idle baseline (before any crypto work in this process)
        val idle=if (idleMs > 0 && !external) meter.measure { Thread.sleep(idleMs) } else null
        val idleW=idle?.let { if (it.energyMilliWattHour.isFinite() && it.durationMs > 0) it.energyMilliWattHour*3.6/(it.durationMs/1000.0) else Double.NaN } ?: Double.NaN
        // 2. warm-up
        val warm=case.warmUp(plain, encrypt, decrypt, warmupMs, smallPower)

        val dir=File(context.getExternalFilesDir(null), "automated/$runId").apply { mkdirs() }
        val csv=File(dir, "${algorithm}_$runId.csv")
        fun num(v: Double) = if (v.isFinite()) String.format(Locale.US, "%.9e", v) else ""
        val rows=StringBuilder(HEADER).append('\n')
        // 3. measured batches
        repeat(rounds) { i ->
            lateinit var b: Batch
            val e=if (external) null else meter.measure { b=runBatch(encrypt, decrypt, inner, targetMs) }
            if (external) b=runBatch(encrypt, decrypt, inner, targetMs)
            check(decrypt()==plain)
            val ej=e?.energyMilliWattHour?.times(3.6) ?: Double.NaN
            val durS=(b.endEpochMs-b.startEpochMs)/1000.0
            val bb=e?.batteryBefore; val ba=e?.batteryAfter
            rows.append(listOf(3, algorithm, 1 shl p, i+1, b.pairs, b.encNs/b.pairs, b.decNs/b.pairs,
                num(ej), num(ej/b.pairs), e?.method ?: "EXTERNAL_PROBE", e?.status ?: "EXTERNAL_PROBE",
                e?.durationMs ?: (b.endEpochMs-b.startEpochMs), e?.samples ?: 0,
                bb?.timestamp ?: "", bb?.levelPercent ?: "", bb?.status ?: "", bb?.let { num(it.temperatureCelsius.toDouble()) } ?: "", bb?.voltageMilliV ?: "",
                ba?.timestamp ?: "", ba?.levelPercent ?: "", ba?.status ?: "", ba?.let { num(it.temperatureCelsius.toDouble()) } ?: "", ba?.voltageMilliV ?: "",
                case.baseAlgorithm, case.keyBits, b.startEpochMs, b.endEpochMs, b.encNs, b.decNs,
                num(idleW), num((ej-idleW*durS)/b.pairs)).joinToString(",")).append('\n')
        }
        csv.writeText(rows.toString())
        val debuggable=(context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        File(csv.path+".json").writeText("""{"algorithm":"$algorithm","algorithm_base":"${case.baseAlgorithm}","key_bits":${case.keyBits},"size_bytes":${1 shl p},"rounds":$rounds,"target_batch_ms":$targetMs,"inner_iterations":$inner,"warmup_ms":$warmupMs,"warmup_small_power":$smallPower,"warmup_pairs":${warm.pairs},"warmup_small_pairs":${warm.smallPairs},"warmup_start_epoch_ms":${warm.startEpochMs},"warmup_end_epoch_ms":${warm.endEpochMs},"idle_ms":$idleMs,"idle_power_w":${if (idleW.isFinite()) idleW else "null"},"idle_status":"${idle?.status ?: "NONE"}","aesgcm_provider":"${javax.crypto.Cipher.getInstance("AES/GCM/NoPadding").provider.name}","debuggable":$debuggable,"vm":"${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}","device":"${Build.MANUFACTURER} ${Build.MODEL}","soc":"${if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else ""}","android":"${Build.VERSION.RELEASE}"}""")
        val lines=csv.readLines()
        check(lines.size==rounds+1 && lines.drop(1).all { it.split(',').size==HEADER.split(',').size })
        println("BENCHMARK_CSV="+csv.path)
    }
    companion object {
        /** Schema 3: the schema-2 columns (enc_ns/dec_ns = mean per operation), then key size, batch window, totals, idle power and dynamic energy. */
        val HEADER = BENCH_EXEC_CSV_HEADER +
            ",algorithm_base,key_bits,batch_start_epoch_ms,batch_end_epoch_ms,enc_total_ns,dec_total_ns,idle_power_w,dynamic_energy_per_pair_j"
    }
}
