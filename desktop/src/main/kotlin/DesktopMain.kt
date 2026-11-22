import com.example.energy.CryptoCase
import com.example.energy.runBatch
import com.example.energy.warmUp
import java.io.File
import java.security.SecureRandom
import java.util.Locale

fun main(args:Array<String>) {
    // algorithm, power, rounds, inner iterations, output path, optional batch target ms,
    // optional warm-up ms, optional small warm-up power
    require(args.size in 5..8) { "Usage: algorithm power rounds inner output.csv [target_batch_ms] [warmup_ms] [warmup_small_power]" }
    val algorithm=args[0]; val power=args[1].toInt(); val rounds=args[2].toInt(); val inner=args[3].toInt()
    val targetMs=args.getOrNull(5)?.toLong() ?: 0L
    // Time-based warm-up before the first measured batch (0 = only the 3 checked round trips, as before).
    val warmupMs=args.getOrNull(6)?.toLong() ?: 0L
    // If > 0, the warm-up first runs warmup_ms on a 2^warmup_small_power-byte input, then warmup_ms on
    // the measured size. Small inputs reach the JIT thresholds quickly (many calls), which matters for
    // code whose fast path needs a C2-compiled caller (e.g. SunJCE AES-GCM intrinsics).
    val warmupSmallPower=args.getOrNull(7)?.toInt() ?: 0
    require(power in 10..20 && rounds in 1..100 && inner in 1..100000 && targetMs in 0..600000 && warmupMs in 0..600000)
    require(warmupSmallPower == 0 || warmupSmallPower in 6..20)
    val output=File(args[4]); require(!output.exists()) { "Output exists; choose a new filename" }
    val case=CryptoCase(algorithm, SecureRandom())
    val baseAlgorithm=case.baseAlgorithm; val keyBits=case.keyBits
    val plain="A".repeat(1 shl power)
    val (encrypt, decrypt) = case.build(plain)
    // Time-based warm-up: same encrypt+decrypt loop as the measured batches, not recorded.
    val warm=case.warmUp(plain, encrypt, decrypt, warmupMs, warmupSmallPower)
    val warmupStartEpoch=warm.startEpochMs; val warmupEndEpoch=warm.endEpochMs
    val warmupPairs=warm.pairs; val warmupSmallPairs=warm.smallPairs
    output.absoluteFile.parentFile.mkdirs()
    output.writeText("algorithm,size_bytes,round,inner_iterations,batch_start_epoch_ms,batch_end_epoch_ms,enc_total_ns,dec_total_ns,enc_mean_ns,dec_mean_ns,energy_j,energy_status\n")
    val manifest=File(output.path+".json")
    manifest.writeText("""{"java_version":"${System.getProperty("java.version")}","os":"${System.getProperty("os.name")}","arch":"${System.getProperty("os.arch")}","aesgcm_provider":"${javax.crypto.Cipher.getInstance("AES/GCM/NoPadding").provider.name}","algorithm_base":"$baseAlgorithm","key_bits":$keyBits,"warmup_rounds":3,"warmup_ms":$warmupMs,"warmup_pairs":$warmupPairs,"warmup_small_power":$warmupSmallPower,"warmup_small_pairs":$warmupSmallPairs,"warmup_start_epoch_ms":$warmupStartEpoch,"warmup_end_epoch_ms":$warmupEndEpoch,"jvm_args":"${java.lang.management.ManagementFactory.getRuntimeMXBean().inputArguments.joinToString(" ").replace("\\","/").replace("\"","'")}","energy_source":"external PowerJoular required"}""")
    repeat(rounds) { round ->
        val batch=runBatch(encrypt, decrypt, inner, targetMs)
        check(decrypt()==plain)
        output.appendText(listOf(algorithm,1 shl power,round+1,batch.pairs,batch.startEpochMs,batch.endEpochMs,batch.encNs,batch.decNs,
            String.format(Locale.US,"%.3f",batch.encNs.toDouble()/batch.pairs),String.format(Locale.US,"%.3f",batch.decNs.toDouble()/batch.pairs),"","NOT_MEASURED").joinToString(",")+"\n")
    }
    println("CSV: "+output.absolutePath)
}
