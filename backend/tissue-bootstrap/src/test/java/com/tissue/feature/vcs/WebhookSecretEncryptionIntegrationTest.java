package com.tissue.feature.vcs;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.vcs.application.port.repository.ProjectVcsIntegrationRepository;
import com.tissue.feature.vcs.application.service.WebhookSecretEncryptionBackfill;
import com.tissue.feature.vcs.domain.ProjectVcsIntegration;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.global.crypto.EncryptedStringConverter;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifies that webhook secrets are unreadable in the database itself. The guarantee depends on Hibernate
 * resolving the converter as a Spring bean so it receives the encryptor, which only a real persistence
 * round-trip can confirm.
 */
class WebhookSecretEncryptionIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectVcsIntegrationRepository integrationRepository;

    @Autowired
    private WebhookSecretEncryptionBackfill backfill;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final String PROJECT_KEY = "PROJ";
    private static final String SECRET = "s3cr3t-webhook-signing-key";

    @Nested
    @DisplayName("store a secret")
    class StoreSecret {

        @Test
        @DisplayName("success: the raw column holds no plain text")
        void columnHoldsNoPlainText() {
            // given
            integrationRepository.save(ProjectVcsIntegration.create(VcsProvider.GITHUB, PROJECT_KEY, SECRET));

            // when
            String stored = readRawSecret(PROJECT_KEY);

            // then
            assertThat(stored).startsWith(EncryptedStringConverter.PREFIX).doesNotContain(SECRET);
        }

        @Test
        @DisplayName("success: the secret reads back intact through the repository")
        void secretReadsBackIntact() {
            // given
            integrationRepository.save(ProjectVcsIntegration.create(VcsProvider.GITHUB, PROJECT_KEY, SECRET));

            // when
            ProjectVcsIntegration found = integrationRepository
                    .findByProjectKeyAndProvider(PROJECT_KEY, VcsProvider.GITHUB)
                    .orElseThrow();

            // then
            assertThat(found.getWebhookSecret()).isEqualTo(SECRET);
        }
    }

    @Nested
    @DisplayName("upgrade an existing instance")
    class UpgradeExistingInstance {

        @Test
        @DisplayName("success: a secret stored before encryption existed is encrypted in place")
        void encryptsLegacyPlainTextSecret() {
            // given
            insertLegacyPlainTextIntegration();

            // when
            backfill.run(new DefaultApplicationArguments());

            // then
            assertThat(readRawSecret(PROJECT_KEY))
                    .startsWith(EncryptedStringConverter.PREFIX)
                    .doesNotContain(SECRET);
            assertThat(integrationRepository
                            .findByProjectKeyAndProvider(PROJECT_KEY, VcsProvider.GITHUB)
                            .orElseThrow()
                            .getWebhookSecret())
                    .isEqualTo(SECRET);
        }

        @Test
        @DisplayName("success: running again leaves already-encrypted secrets untouched")
        void isIdempotent() {
            // given
            insertLegacyPlainTextIntegration();
            backfill.run(new DefaultApplicationArguments());
            String afterFirstRun = readRawSecret(PROJECT_KEY);

            // when
            backfill.run(new DefaultApplicationArguments());

            // then
            assertThat(readRawSecret(PROJECT_KEY)).isEqualTo(afterFirstRun);
        }
    }

    private void insertLegacyPlainTextIntegration() {
        transactionTemplate.executeWithoutResult(status -> em.createNativeQuery("""
                        INSERT INTO project_vcs_integration (vcs_provider, project_key, webhook_secret, sync_enabled)
                        VALUES ('GITHUB', :projectKey, :secret, true)
                        """)
                .setParameter("projectKey", PROJECT_KEY)
                .setParameter("secret", SECRET)
                .executeUpdate());
    }

    private String readRawSecret(String projectKey) {
        return transactionTemplate.execute(status -> (String) em.createNativeQuery(
                        "SELECT webhook_secret FROM project_vcs_integration WHERE project_key = :projectKey")
                .setParameter("projectKey", projectKey)
                .getSingleResult());
    }
}
