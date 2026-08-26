package com.tissue.global.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Supplies the encryptor used for secrets that must be readable again, such as webhook signing keys, which
 * cannot be hashed because verifying a signature means recomputing it.
 *
 * <p>The key defaults to the JWT secret so that upgrading an existing instance needs no new configuration.
 * Set a dedicated key to decouple the two: rotating the JWT secret otherwise leaves stored secrets
 * undecryptable, and they would have to be regenerated and re-registered with the provider.
 */
@Configuration
public class EncryptionConfig {

    /**
     * Not a secret. It seeds key derivation, and changing it invalidates everything already encrypted.
     */
    private static final String DEFAULT_SALT = "5c0744940b5c369b";

    @Bean
    public TextEncryptor tissueTextEncryptor(
            @Value("${tissue.security.encryption.key:}") String encryptionKey,
            @Value("${tissue.security.jwt.secret:}") String jwtSecret,
            @Value("${tissue.security.encryption.salt:" + DEFAULT_SALT + "}") String salt) {

        String password = encryptionKey.isBlank() ? jwtSecret : encryptionKey;
        if (password.isBlank()) {
            throw new IllegalStateException("No encryption key available. Set tissue.security.encryption.key "
                    + "(TISSUE_ENCRYPTION_KEY) or tissue.security.jwt.secret (JWT_SECRET).");
        }

        return Encryptors.delux(password, salt);
    }
}
