import com.example.energy.AES;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Arrays;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// Test-only key and IV from Heitor's AES_Algorithm_encrypt.c.
// The independent JCE reference reproduces the C parameters, not the C executable.
class AesCbcCompatibility {
    public static void main(String[] args) throws Exception {
        var hex = HexFormat.of();
        byte[] key = hex.parseHex("c31f2856743dc395a95a4bc1e7a65af057abcae768ced4aba1b88a2493daf800");
        byte[] iv = hex.parseHex("000102030405060708090a0b0c0d0e0f");
        for (int size : new int[]{0, 15, 16, 17, 2048, 4096, 8192}) {
            String plain = "A".repeat(size);
            byte[] bytes = plain.getBytes(StandardCharsets.UTF_8);
            var ref = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var secret = new SecretKeySpec(key, "AES");
            var params = new IvParameterSpec(iv);
            ref.init(Cipher.ENCRYPT_MODE, secret, params);
            byte[] expected = ref.doFinal(bytes);
            String desktop = AES.INSTANCE.encrypt(plain, key, iv);
            byte[] actual = Base64.getDecoder().decode(desktop);
            ref.init(Cipher.DECRYPT_MODE, secret, params);
            if (!Arrays.equals(expected, actual) ||
                !Arrays.equals(ref.doFinal(actual), bytes) ||
                !AES.INSTANCE.decrypt(Base64.getEncoder().encodeToString(expected), key, iv).equals(plain)) {
                throw new AssertionError("Mismatch at " + size + " bytes");
            }
            System.out.printf("%d bytes: ciphertext match and both decryptions PASS%n", size);
        }
    }
}
