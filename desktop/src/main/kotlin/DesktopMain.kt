import com.example.energy.lwc.BcLwcAead
import com.example.energy.lwc.LwcSpecs
import org.bouncycastle.crypto.modes.AEADCipher
import org.bouncycastle.crypto.engines.AsconAEAD128
import org.bouncycastle.crypto.engines.ElephantEngine
import org.bouncycastle.crypto.engines.GiftCofbEngine
import org.bouncycastle.crypto.engines.Grain128AEADEngine
import org.bouncycastle.crypto.engines.XoodyakEngine
import com.example.energy.AES
import com.example.energy.AESGcm
import com.example.energy.chacha20.ChaCha20Cipher
import com.example.energy.rsa.RsaLite
import com.example.energy.elgamal.ElGamalLite
import org.bouncycastle.crypto.params.RSAKeyParameters
import org.bouncycastle.crypto.params.ElGamalPublicKeyParameters
import org.bouncycastle.crypto.params.ElGamalPrivateKeyParameters
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.util.encoders.Base64
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
    val random=SecureRandom(); val key=ByteArray(32).also(random::nextBytes); val iv=ByteArray(16)
    // Optional key size in the algorithm name: aes128/192/256, aesgcm128.., aesgcmbc128..,
    // rsa1024/2048/3072/4096, elgamal768/1024/2048/3072/4096. Without a number: AES-256,
    // RSA-2048, ElGamal-2048. The RSA/ElGamal hybrids always use AES-256 for the bulk data.
    val sized=Regex("^(aesgcmbc|aesgcm|aes|rsa|elgamal)(\\d+)$").matchEntire(algorithm)
    val baseAlgorithm=sized?.groupValues?.get(1) ?: algorithm
    val keyBits=sized?.groupValues?.get(2)?.toInt() ?: when(baseAlgorithm) {
        "aes", "aesgcm", "aesgcmbc" -> 256; "rsa" -> 2048; "elgamal" -> ElGamalLite.DEFAULT_BITS; else -> 0 }
    when(baseAlgorithm) {
        "aes", "aesgcm", "aesgcmbc" -> require(keyBits in setOf(128, 192, 256)) { "AES key size must be 128, 192 or 256" }
        "rsa" -> require(keyBits in setOf(1024, 2048, 3072, 4096)) { "RSA size must be 1024, 2048, 3072 or 4096" }
        "elgamal" -> require(keyBits in ElGamalLite.SUPPORTED_BITS) { "ElGamal group must be one of ${ElGamalLite.SUPPORTED_BITS}" }
    }
    val symKey=ByteArray(if (baseAlgorithm.startsWith("aes")) keyBits / 8 else 32).also(random::nextBytes)
    fun build(plain: String): Pair<() -> Unit, () -> String> {
    var ciphertext=""; var wrapped=ByteArray(0)
    val encrypt:()->Unit
    val decrypt:()->String
    when(baseAlgorithm) {
        "aes" -> { encrypt={ciphertext=AES.encrypt(plain,symKey,iv)}; decrypt={AES.decrypt(ciphertext,symKey,iv)} }
        "aesgcm" -> { encrypt={ciphertext=AESGcm.encrypt(plain,symKey)}; decrypt={AESGcm.decrypt(ciphertext,symKey)} }
        // AES-256-GCM through Bouncy Castle (like every other algorithm here) instead of the JCA
        // provider (SunJCE on desktop JVMs, Conscrypt on Android). Same envelope as AESGcm:
        // Base64(IV(12) || CIPHERTEXT || TAG(16)), random IV per encryption.
        "aesgcmbc" -> {
            fun gcm(forEncryption: Boolean, nonce: ByteArray) =
                GCMBlockCipher.newInstance(AESEngine.newInstance()).also {
                    it.init(forEncryption, AEADParameters(KeyParameter(symKey), 128, nonce))
                }
            encrypt = {
                val nonce = ByteArray(12).also(random::nextBytes)
                val pt = plain.toByteArray(Charsets.UTF_8)
                val c = gcm(true, nonce)
                val out = ByteArray(12 + c.getOutputSize(pt.size))
                System.arraycopy(nonce, 0, out, 0, 12)
                var n = c.processBytes(pt, 0, pt.size, out, 12)
                n += c.doFinal(out, 12 + n)
                ciphertext = Base64.toBase64String(out, 0, 12 + n)
            }
            decrypt = {
                val all = Base64.decode(ciphertext)
                val c = gcm(false, all.copyOfRange(0, 12))
                val out = ByteArray(c.getOutputSize(all.size - 12))
                var n = c.processBytes(all, 12, all.size - 12, out, 0)
                n += c.doFinal(out, n)
                String(out, 0, n, Charsets.UTF_8)
            }
        }
        "chacha20" -> { val nonce=ByteArray(12).also(random::nextBytes)
            encrypt={ciphertext=ChaCha20Cipher.encrypt(plain,key,nonce)}
            decrypt={ChaCha20Cipher.decrypt(ciphertext,key,nonce)} }
        "rsa" -> { val pair=RsaLite.generateKeyPair(keyBits)
            encrypt={random.nextBytes(key);random.nextBytes(iv);ciphertext=AES.encrypt(plain,key,iv);wrapped=RsaLite.encryptSmall(key+iv,pair.public as RSAKeyParameters)}
            decrypt={val b=RsaLite.decryptSmall(wrapped,pair.private as RSAKeyParameters);AES.decrypt(ciphertext,b.copyOfRange(0,32),b.copyOfRange(32,48))} }
        "elgamal" -> { val pair=ElGamalLite.generateKeyPair(keyBits)
            encrypt={random.nextBytes(key);random.nextBytes(iv);ciphertext=AES.encrypt(plain,key,iv);wrapped=ElGamalLite.encryptSmall(key+iv,pair.public as ElGamalPublicKeyParameters)}
            decrypt={val b=ElGamalLite.decryptSmall(wrapped,pair.private as ElGamalPrivateKeyParameters);AES.decrypt(ciphertext,b.copyOfRange(0,32),b.copyOfRange(32,48))} }
        "ascon", "elephant", "giftcofb", "grain128", "xoodyak" -> {
            val implementation: Pair<com.example.energy.lwc.AeadSpec, () -> AEADCipher> = when (algorithm) {
                "ascon" -> LwcSpecs.ASCON_128 to { AsconAEAD128() }
                "elephant" -> LwcSpecs.ELEPHANT to { ElephantEngine(ElephantEngine.ElephantParameters.elephant200) }
                "giftcofb" -> LwcSpecs.GIFT_COFB to { GiftCofbEngine() }
                "grain128" -> LwcSpecs.GRAIN128 to { Grain128AEADEngine() }
                else -> LwcSpecs.XOODYAK to { XoodyakEngine() }
            }
            val (spec, engine) = implementation
            val aeadKey = ByteArray(spec.keyBytes).also(random::nextBytes)
            val nonce = ByteArray(spec.nonceBytes)
            encrypt = {
                random.nextBytes(nonce)
                ciphertext = BcLwcAead.encryptUtf8(engine(), spec, plain, aeadKey, nonce)
            }
            decrypt = { BcLwcAead.decryptUtf8(engine(), spec, ciphertext, aeadKey) }
        }
        else -> error("Unknown algorithm")
    }
    return encrypt to decrypt
    }
    val warmupStartEpoch=System.currentTimeMillis()
    var warmupSmallPairs=0
    if (warmupSmallPower > 0) {
        val smallPlain="A".repeat(1 shl warmupSmallPower)
        val (enc, dec) = build(smallPlain)
        repeat(3) { enc(); check(dec()==smallPlain) }
        val t0=System.nanoTime()
        while(System.nanoTime()-t0<warmupMs*1_000_000) { enc(); dec(); warmupSmallPairs++ }
    }
    val plain="A".repeat(1 shl power)
    val (encrypt, decrypt) = build(plain)
    repeat(3) { encrypt();check(decrypt()==plain) }
    // Time-based warm-up: same encrypt+decrypt loop as the measured batches, not recorded.
    val warmupStart=System.nanoTime(); var warmupPairs=0
    while(System.nanoTime()-warmupStart<warmupMs*1_000_000) { encrypt(); decrypt(); warmupPairs++ }
    val warmupEndEpoch=System.currentTimeMillis()
    check(decrypt()==plain)
    output.absoluteFile.parentFile.mkdirs()
    output.writeText("algorithm,size_bytes,round,inner_iterations,batch_start_epoch_ms,batch_end_epoch_ms,enc_total_ns,dec_total_ns,enc_mean_ns,dec_mean_ns,energy_j,energy_status\n")
    val manifest=File(output.path+".json")
    manifest.writeText("""{"java_version":"${System.getProperty("java.version")}","os":"${System.getProperty("os.name")}","arch":"${System.getProperty("os.arch")}","aesgcm_provider":"${javax.crypto.Cipher.getInstance("AES/GCM/NoPadding").provider.name}","algorithm_base":"$baseAlgorithm","key_bits":$keyBits,"warmup_rounds":3,"warmup_ms":$warmupMs,"warmup_pairs":$warmupPairs,"warmup_small_power":$warmupSmallPower,"warmup_small_pairs":$warmupSmallPairs,"warmup_start_epoch_ms":$warmupStartEpoch,"warmup_end_epoch_ms":$warmupEndEpoch,"jvm_args":"${java.lang.management.ManagementFactory.getRuntimeMXBean().inputArguments.joinToString(" ").replace("\\","/").replace("\"","'")}","energy_source":"external PowerJoular required"}""")
    repeat(rounds) { round ->
        var enc=0L;var dec=0L;var iterations=0
        val epochStart=System.currentTimeMillis();val start=System.nanoTime()
        do {
            var t=System.nanoTime();repeat(inner){encrypt()};enc+=System.nanoTime()-t
            t=System.nanoTime();repeat(inner){decrypt()};dec+=System.nanoTime()-t
            iterations+=inner
        } while(System.nanoTime()-start<targetMs*1_000_000)
        val epochEnd=System.currentTimeMillis()
        check(decrypt()==plain)
        output.appendText(listOf(algorithm,1 shl power,round+1,iterations,epochStart,epochEnd,enc,dec,
            String.format(Locale.US,"%.3f",enc.toDouble()/iterations),String.format(Locale.US,"%.3f",dec.toDouble()/iterations),"","NOT_MEASURED").joinToString(",")+"\n")
    }
    println("CSV: "+output.absolutePath)
}
