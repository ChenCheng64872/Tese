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
import java.io.File
import java.security.SecureRandom
import java.util.Locale

fun main(args:Array<String>) {
    // algorithm, power, rounds, inner iterations, output path, optional batch target ms
    require(args.size in 5..6) { "Usage: algorithm power rounds inner output.csv [target_batch_ms]" }
    val algorithm=args[0]; val power=args[1].toInt(); val rounds=args[2].toInt(); val inner=args[3].toInt()
    val targetMs=args.getOrNull(5)?.toLong() ?: 0L
    require(power in 10..20 && rounds in 1..100 && inner in 1..100000 && targetMs in 0..600000)
    val output=File(args[4]); require(!output.exists()) { "Output exists; choose a new filename" }
    val plain="A".repeat(1 shl power)
    val random=SecureRandom(); val key=ByteArray(32).also(random::nextBytes); val iv=ByteArray(16)
    var ciphertext=""; var wrapped=ByteArray(0)
    val encrypt:()->Unit
    val decrypt:()->String
    when(algorithm) {
        "aes" -> { encrypt={ciphertext=AES.encrypt(plain,key,iv)}; decrypt={AES.decrypt(ciphertext,key,iv)} }
        "aesgcm" -> { encrypt={ciphertext=AESGcm.encrypt(plain,key)}; decrypt={AESGcm.decrypt(ciphertext,key)} }
        "chacha20" -> { val nonce=ByteArray(12).also(random::nextBytes)
            encrypt={ciphertext=ChaCha20Cipher.encrypt(plain,key,nonce)}
            decrypt={ChaCha20Cipher.decrypt(ciphertext,key,nonce)} }
        "rsa" -> { val pair=RsaLite.generateKeyPair(2048)
            encrypt={random.nextBytes(key);random.nextBytes(iv);ciphertext=AES.encrypt(plain,key,iv);wrapped=RsaLite.encryptSmall(key+iv,pair.public as RSAKeyParameters)}
            decrypt={val b=RsaLite.decryptSmall(wrapped,pair.private as RSAKeyParameters);AES.decrypt(ciphertext,b.copyOfRange(0,32),b.copyOfRange(32,48))} }
        "elgamal" -> { val pair=ElGamalLite.generateKeyPair()
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
    repeat(3) { encrypt();check(decrypt()==plain) }
    output.absoluteFile.parentFile.mkdirs()
    output.writeText("algorithm,size_bytes,round,inner_iterations,batch_start_epoch_ms,batch_end_epoch_ms,enc_total_ns,dec_total_ns,enc_mean_ns,dec_mean_ns,energy_j,energy_status\n")
    val manifest=File(output.path+".json")
    manifest.writeText("""{"java_version":"${System.getProperty("java.version")}","os":"${System.getProperty("os.name")}","arch":"${System.getProperty("os.arch")}","aesgcm_provider":"${javax.crypto.Cipher.getInstance("AES/GCM/NoPadding").provider.name}","warmup_rounds":3,"energy_source":"external PowerJoular required"}""")
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
