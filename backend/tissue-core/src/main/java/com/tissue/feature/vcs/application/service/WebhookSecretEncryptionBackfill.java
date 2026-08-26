package com.tissue.feature.vcs.application.service;

import com.tissue.feature.vcs.application.port.repository.ProjectVcsIntegrationRepository;
import com.tissue.global.crypto.EncryptedStringConverter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encrypts webhook secrets that were stored before encryption existed.
 *
 * <p>Runs on startup rather than as a schema migration because the values can only be produced by the
 * application's encryptor, not by SQL. Selecting on the absence of the version prefix makes it idempotent,
 * so it is a no-op on every boot after the first.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookSecretEncryptionBackfill implements ApplicationRunner {

    private final ProjectVcsIntegrationRepository integrationRepository;
    private final EncryptedStringConverter converter;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Object[]> plaintextRows =
                integrationRepository.findRowsWithPlaintextSecret(EncryptedStringConverter.PREFIX + "%");

        if (plaintextRows.isEmpty()) {
            return;
        }

        for (Object[] row : plaintextRows) {
            Long id = ((Number) row[0]).longValue();
            String plaintext = String.valueOf(row[1]);
            integrationRepository.applyEncryptedSecret(id, converter.encrypt(plaintext));
        }

        log.info("Encrypted {} webhook secret(s) that were stored in plain text", plaintextRows.size());
    }
}
