package com.example.energy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
@RunWith(AndroidJUnit4::class)
class CryptoBenchmarkInstrumentedTest {
    @Test fun runbenchmark() {
        val a=InstrumentationRegistry.getArguments()
        fun int(name:String, default:Int)=a.getString(name)?.toInt() ?: default
        val algorithm=a.getString("algorithm") ?: "aesgcm"
        val runId=a.getString("run_id") ?: "manual_"+System.currentTimeMillis()
        require(runId.matches(Regex("[A-Za-z0-9_-]+")))
        val p=int("contentsize",10); val rounds=int("rounds",1)
        val inner=int("inner_iterations",100); val warmup=int("warmup_rounds",3)
        require(p in 10..20 && rounds in 1..100 && inner in 1..100000 && warmup in 1..100)
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        println("BENCHMARK_PROVIDER=" + if (algorithm == "aesgcm") javax.crypto.Cipher.getInstance("AES/GCM/NoPadding").provider.name else "BouncyCastle-lightweight-1.82")
        val fileName=algorithm+"_"+runId+".csv"
        val dir="automated/"+runId
        BenchmarkRunner.externalEnergyProbe.set(a.getString("external_energy_probe") == "true")
        val path = try { when(algorithm) {
            "aes" -> AesBenchmark.runRangeAndLog(context,p,p,rounds,inner,warmup,fileName,dir)
            "aesgcm" -> AesGcmBenchmark.runRangeAndLog(context,p,p,rounds,inner,warmup,fileName,dir)
            "chacha20" -> ChaCha20Benchmark.runRangeAndLog(context,p,p,rounds,inner,warmup,fileName,dir)
            "rsa" -> RsaHybridBenchmark.runRangeAndLog(context,p,p,rounds,inner,warmup,fileName,dir)
            "elgamal" -> ElGamalBenchmark.runRangeAndLog(context,p,p,rounds,inner,warmup,fileName,dir)
            "ascon" -> AsconLwcBenchmark.runRangeAndLog(context,p,p,rounds,inner,warmup,fileName,dir)
            "elephant" -> ElephantBenchmark.runRangeAndLog(context,p,p,rounds,inner,warmup,fileName,dir)
            "giftcofb" -> GiftCofbBenchmark.runRangeAndLog(context,p,p,rounds,inner,warmup,fileName,dir)
            "grain128" -> Grain128AeadBenchmark.runRangeAndLog(context,p,p,rounds,inner,warmup,fileName,dir)
            "xoodyak" -> XoodyakBenchmark.runRangeAndLog(context,p,p,rounds,inner,warmup,fileName,dir)
            else -> error("Unknown algorithm: "+algorithm)
        }
        } finally { BenchmarkRunner.externalEnergyProbe.remove() }
        val lines=File(path).readLines()
        check(lines.first()==BENCH_EXEC_CSV_HEADER && lines.size==rounds+1)
        val header=lines.first().split(',')
        lines.drop(1).forEach { line ->
            val cols=line.split(','); check(cols.size==header.size)
            check(cols[header.indexOf("enc_ns")].toLong()>0)
            check(cols[header.indexOf("dec_ns")].toLong()>0)
        }
        println("BENCHMARK_CSV="+path)
    }
}
