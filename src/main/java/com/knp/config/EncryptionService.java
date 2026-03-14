package com.knp.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM symmetric encryption for sensitive fields stored in the database.
 *
 * DB format: {enc}<IV_base64>:<ciphertext+tag_base64>
 *
 * The {enc} prefix lets converters distinguish encrypted values from plaintext
 * left over before migration, so reads are safe before the migration runs.
 *
 * Key must be a Base64-encoded 32-byte (256-bit) secret, supplied via the
 * APP_ENCRYPTION_KEY environment variable (see app.encryption.key property).
 *
 * Generate a key:  openssl rand -base64 32
 */
@Slf4j
@Service
public class EncryptionService {

    static final String ENC_PREFIX = "{enc}";
    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    IV_BYTES   = 12;   // 96-bit IV recommended for GCM
    private static final int    TAG_BITS   = 128;  // GCM authentication tag length

    private final String base64Key;
    private SecretKeySpec secretKey;

    public EncryptionService(@Value("${app.encryption.key}") String base64Key) {
        this.base64Key = base64Key;
    }

    @PostConstruct
    void init() {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "app.encryption.key is not configured. " +
                    "Set the APP_ENCRYPTION_KEY environment variable. " +
                    "Generate a key with: openssl rand -base64 32");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.encryption.key is not valid Base64", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "app.encryption.key must decode to exactly 32 bytes (256 bits), got " + keyBytes.length);
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        log.info("EncryptionService initialised with AES-256-GCM");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return plaintext;
        if (isEncrypted(plaintext)) return plaintext; // idempotent

        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            String ivB64  = Base64.getEncoder().encodeToString(iv);
            String ctB64  = Base64.getEncoder().encodeToString(ciphertext);
            return ENC_PREFIX + ivB64 + ":" + ctB64;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || stored.isBlank()) return stored;
        if (!isEncrypted(stored)) return stored; // plaintext passthrough (pre-migration)

        try {
            String payload  = stored.substring(ENC_PREFIX.length());
            int    sep      = payload.indexOf(':');
            byte[] iv         = Base64.getDecoder().decode(payload.substring(0, sep));
            byte[] ciphertext = Base64.getDecoder().decode(payload.substring(sep + 1));

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENC_PREFIX);
    }
}
