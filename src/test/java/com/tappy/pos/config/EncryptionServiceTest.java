package com.tappy.pos.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EncryptionServiceTest {

    // 32-byte dev key (same as application-dev.properties)
    private static final String DEV_KEY = "dGhpcyBpcyBhIGRldiBrZXkgZm9yIHRlc3RpbmcgMTI=";

    private EncryptionService service;

    @BeforeEach
    void setUp() {
        service = new EncryptionService(DEV_KEY);
        service.init();
    }

    @Test
    void encrypt_then_decrypt_returns_original() {
        String plaintext = "Super$ecretP@ssw0rd!";
        String encrypted = service.encrypt(plaintext);

        assertThat(encrypted).startsWith(EncryptionService.ENC_PREFIX);
        assertThat(service.decrypt(encrypted)).isEqualTo(plaintext);
    }

    @Test
    void encrypt_is_non_deterministic() {
        String enc1 = service.encrypt("same-value");
        String enc2 = service.encrypt("same-value");
        // Different IVs → different ciphertexts every time
        assertThat(enc1).isNotEqualTo(enc2);
    }

    @Test
    void decrypt_plaintext_passthrough_for_pre_migration_values() {
        String plaintext = "old-plaintext-password";
        assertThat(service.decrypt(plaintext)).isEqualTo(plaintext);
    }

    @Test
    void encrypt_is_idempotent_already_encrypted_value() {
        String encrypted = service.encrypt("value");
        assertThat(service.encrypt(encrypted)).isEqualTo(encrypted);
    }

    @Test
    void null_and_blank_values_pass_through_unchanged() {
        assertThat(service.encrypt(null)).isNull();
        assertThat(service.encrypt("")).isEqualTo("");
        assertThat(service.decrypt(null)).isNull();
        assertThat(service.decrypt("")).isEqualTo("");
    }

    @Test
    void long_api_key_round_trip() {
        String longKey = "a".repeat(400); // 400-char API key
        assertThat(service.decrypt(service.encrypt(longKey))).isEqualTo(longKey);
    }

    @Test
    void invalid_key_length_throws_on_init() {
        EncryptionService bad = new EncryptionService("dGVzdA=="); // 4 bytes, not 32
        assertThatThrownBy(bad::init).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void missing_key_throws_on_init() {
        EncryptionService bad = new EncryptionService("");
        assertThatThrownBy(bad::init).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_ENCRYPTION_KEY");
    }

    @Test
    void invalid_base64_key_throws_on_init() {
        // Not valid Base64
        EncryptionService bad = new EncryptionService("not-valid-base64!!!");
        assertThatThrownBy(bad::init).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not valid Base64");
    }

    @Test
    void isEncrypted_returns_true_for_enc_prefix() {
        assertThat(service.isEncrypted("{enc}somedata")).isTrue();
    }

    @Test
    void isEncrypted_returns_false_for_plain_value() {
        assertThat(service.isEncrypted("plaintext")).isFalse();
    }

    @Test
    void isEncrypted_returns_false_for_null() {
        assertThat(service.isEncrypted(null)).isFalse();
    }

    @Test
    void decrypt_throws_on_tampered_ciphertext() {
        String encrypted = service.encrypt("original");
        // Corrupt the ciphertext portion
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "XXXX";
        assertThatThrownBy(() -> service.decrypt(tampered))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decryption failed");
    }

    @Test
    void blank_value_passes_through_unchanged_for_encrypt() {
        assertThat(service.encrypt("   ")).isEqualTo("   ");
    }

    @Test
    void blank_value_passes_through_unchanged_for_decrypt() {
        assertThat(service.decrypt("   ")).isEqualTo("   ");
    }
}
