package lucas.basemodel.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptionUtils {

    private static final Logger log = LoggerFactory.getLogger(EncryptionUtils.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static SecretKeySpec secretKeySpec;
    private static IvParameterSpec deterministicIvSpec;

    @Value("${application.security.encryption.key:#{null}}")
    private String configKey;

    @PostConstruct
    public void init() {
        String key = configKey;
        if (key == null || key.trim().isEmpty()) {
            key = System.getenv("DB_ENCRYPTION_KEY");
        }
        if (key == null || key.trim().isEmpty()) {
            // Use JWT secret as fallback key if available, otherwise use a default fallback
            key = System.getenv("JWT_SECRET");
            if (key == null || key.trim().isEmpty()) {
                key = "fallback-default-encryption-key-for-local-dev-only";
                log.warn("⚠️ CRITICAL: No DB_ENCRYPTION_KEY or JWT_SECRET environment variables configured. Using default fallback key.");
            } else {
                log.info("ℹ️ Using JWT_SECRET as fallback for database encryption key.");
            }
        }

        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyHash = sha.digest(keyBytes);
            
            // Set 256-bit AES key
            secretKeySpec = new SecretKeySpec(keyHash, ALGORITHM);

            // Set deterministic IV using the first 16 bytes of the key hash
            byte[] iv = new byte[16];
            System.arraycopy(keyHash, 0, iv, 0, 16);
            deterministicIvSpec = new IvParameterSpec(iv);
            
            log.info("🔒 Cryptographic system initialized successfully (AES-256).");
        } catch (Exception e) {
            log.error("❌ Failed to initialize EncryptionUtils: ", e);
            throw new RuntimeException("Cryptographic subsystem initialization failed", e);
        }
    }

    /**
     * Encrypts a string value using randomized IV. Recommended for fields that do not need database queries.
     */
    public static String encryptRandom(String value) {
        if (value == null) {
            return null;
        }
        try {
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Encryption failed for value: ", e);
            return value;
        }
    }

    /**
     * Decrypts a string value that was encrypted using randomized IV.
     */
    public static String decryptRandom(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.trim().isEmpty()) {
            return encryptedValue;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedValue);
            if (combined.length <= 16) {
                // Not a valid encrypted value (must contain at least IV + ciphertext)
                return encryptedValue;
            }

            byte[] iv = new byte[16];
            System.arraycopy(combined, 0, iv, 0, 16);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            byte[] encrypted = new byte[combined.length - 16];
            System.arraycopy(combined, 16, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Graceful fallback for values that were stored in plaintext before encryption was enabled
            log.debug("Decryption failed (returning plaintext value): {}", e.getMessage());
            return encryptedValue;
        }
    }

    /**
     * Encrypts a string value using a deterministic fixed IV. Required for exact matching database lookups.
     */
    public static String encryptDeterministic(String value) {
        if (value == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, deterministicIvSpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Deterministic encryption failed for value: ", e);
            return value;
        }
    }

    /**
     * Decrypts a string value that was encrypted using a deterministic fixed IV.
     */
    public static String decryptDeterministic(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.trim().isEmpty()) {
            return encryptedValue;
        }
        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedValue);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, deterministicIvSpec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("Deterministic decryption failed (returning plaintext value): {}", e.getMessage());
            return encryptedValue;
        }
    }
}
