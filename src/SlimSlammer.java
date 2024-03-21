/*import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class SlimSlammer {
    private static final String ALGORITHM = "AES";
    // Note: In a real application, you'd need to securely store and manage this key
    private static final byte[] KEY = "YourSecretKeyDuh".getBytes(); // Must be 16 bytes

    public static String encrypt(String valueToEnc) throws Exception {
        SecretKey key = new SecretKeySpec(KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedByteValue = cipher.doFinal(valueToEnc.getBytes("utf-8"));
        return Base64.getEncoder().encodeToString(encryptedByteValue);
    }

    public static String decrypt(String encryptedValue) throws Exception {
        SecretKey key = new SecretKeySpec(KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decryptedValue64 = Base64.getDecoder().decode(encryptedValue);
        byte[] decryptedByteValue = cipher.doFinal(decryptedValue64);
        return new String(decryptedByteValue, "utf-8");
    }
}*/