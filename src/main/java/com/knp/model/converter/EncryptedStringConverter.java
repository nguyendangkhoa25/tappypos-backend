package com.knp.model.converter;

import com.knp.config.ApplicationContextProvider;
import com.knp.config.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA converter that transparently encrypts sensitive strings before writing
 * to the database and decrypts them on read.
 *
 * Pre-migration plaintext values (no {enc} prefix) are returned as-is on read
 * and will be encrypted the next time the record is saved.
 */
@Slf4j
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private EncryptionService encryptionService() {
        return ApplicationContextProvider.getBean(EncryptionService.class);
    }

    @Override
    public String convertToDatabaseColumn(String value) {
        if (value == null || value.isBlank()) return value;
        try {
            return encryptionService().encrypt(value);
        } catch (Exception e) {
            log.error("Failed to encrypt field — storing as-is to prevent data loss", e);
            return value;
        }
    }

    @Override
    public String convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) return dbValue;
        try {
            return encryptionService().decrypt(dbValue);
        } catch (Exception e) {
            // Value may be plaintext from before the converter was added; return it as-is.
            // It will be encrypted on the next save.
            log.warn("Failed to decrypt field value — returning as plaintext (pre-migration row?)");
            return dbValue;
        }
    }
}
