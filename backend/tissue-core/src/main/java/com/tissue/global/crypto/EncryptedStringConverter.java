package com.tissue.global.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * Encrypts a column at rest.
 *
 * <p>Stored values carry a version prefix, which serves two purposes: a value written before encryption
 * existed is recognisable and read back as-is, so an upgrade does not break, and a future scheme can be
 * introduced alongside the current one rather than requiring a flag day.
 */
@Component
@Converter
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    public static final String PREFIX = "enc:v1:";

    private final TextEncryptor encryptor;

    @Override
    @Nullable
    public String convertToDatabaseColumn(@Nullable String attribute) {
        if (attribute == null) {
            return null;
        }

        return encrypt(attribute);
    }

    /**
     * Produces the stored form of a value, for callers that write the column directly rather than through
     * an entity.
     */
    public String encrypt(String plaintext) {
        return PREFIX + encryptor.encrypt(plaintext);
    }

    @Override
    @Nullable
    public String convertToEntityAttribute(@Nullable String dbData) {
        if (dbData == null) {
            return null;
        }
        if (!dbData.startsWith(PREFIX)) {
            return dbData;
        }

        try {
            return encryptor.decrypt(dbData.substring(PREFIX.length()));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Stored secret could not be decrypted. The encryption key changed; "
                            + "affected secrets must be regenerated and re-registered with their provider.",
                    e);
        }
    }
}
