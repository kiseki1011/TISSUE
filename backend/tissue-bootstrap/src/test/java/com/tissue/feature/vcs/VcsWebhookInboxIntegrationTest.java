package com.tissue.feature.vcs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.vcs.application.port.repository.VcsWebhookDeliveryRepository;
import com.tissue.feature.vcs.application.service.VcsWebhookDeliveryWriter;
import com.tissue.feature.vcs.domain.VcsWebhookDelivery;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.enums.WebhookDeliveryStatus;
import com.tissue.support.IntegrationTestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Exercises the inbox against a real database, where the deduplication guarantee actually lives: the
 * unique delivery id is a database constraint, not application logic.
 */
class VcsWebhookInboxIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private VcsWebhookDeliveryWriter sut;

    @Autowired
    private VcsWebhookDeliveryRepository deliveryRepository;

    private static final String DELIVERY_ID = "72d3162e-cc78-11e3-81ab-4c9367dc0958";
    private static final String PROJECT_KEY = "PROJ";
    private static final String PAYLOAD = "{\"ref\":\"refs/heads/main\"}";

    @Nested
    @DisplayName("store a delivery")
    class StoreDelivery {

        @Test
        @DisplayName("success: the same delivery id is rejected on redelivery")
        void rejectsRedelivery() {
            // given
            sut.create(VcsProvider.GITHUB, DELIVERY_ID, PROJECT_KEY, "push", PAYLOAD);

            // when & then
            assertThatThrownBy(() -> sut.create(VcsProvider.GITHUB, DELIVERY_ID, PROJECT_KEY, "push", PAYLOAD))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("success: the same delivery id from a different provider is a distinct delivery")
        void allowsSameIdFromAnotherProvider() {
            // given
            Long githubId = sut.create(VcsProvider.GITHUB, DELIVERY_ID, PROJECT_KEY, "push", PAYLOAD);

            // when
            Long gitlabId = sut.create(VcsProvider.GITLAB, DELIVERY_ID, PROJECT_KEY, "push", PAYLOAD);

            // then
            assertThat(gitlabId).isNotEqualTo(githubId);
        }
    }

    @Nested
    @DisplayName("retry a failed delivery")
    class RetryDelivery {

        @Test
        @DisplayName("success: a failed delivery becomes due only once its backoff has elapsed")
        void becomesDueAfterBackoff() {
            // given
            Long id = sut.create(VcsProvider.GITHUB, DELIVERY_ID, PROJECT_KEY, "push", PAYLOAD);
            sut.recordFailure(id, new IllegalStateException("database unavailable"));

            // when
            List<Long> dueNow = sut.findDueForRetry(Instant.now());
            List<Long> dueLater = sut.findDueForRetry(Instant.now().plus(Duration.ofMinutes(5)));

            // then
            assertThat(dueNow).isEmpty();
            assertThat(dueLater).containsExactly(id);
        }

        @Test
        @DisplayName("success: a failure is recorded with its reason and attempt count")
        void recordsFailureDetail() {
            // given
            Long id = sut.create(VcsProvider.GITHUB, DELIVERY_ID, PROJECT_KEY, "push", PAYLOAD);

            // when
            sut.recordFailure(id, new IllegalStateException("database unavailable"));

            // then
            VcsWebhookDelivery stored = deliveryRepository.findById(id).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
            assertThat(stored.getAttemptCount()).isEqualTo(1);
            assertThat(stored.getLastError()).contains("database unavailable");
        }
    }

    @Nested
    @DisplayName("list deliveries")
    class ListDeliveries {

        @Test
        @DisplayName("success: deliveries page newest first")
        void pagesNewestFirst() {
            // given
            Long first = sut.create(VcsProvider.GITHUB, "d-1", PROJECT_KEY, "push", PAYLOAD);
            Long second = sut.create(VcsProvider.GITHUB, "d-2", PROJECT_KEY, "push", PAYLOAD);
            Long third = sut.create(VcsProvider.GITHUB, "d-3", PROJECT_KEY, "pull_request", PAYLOAD);

            // when
            Page<VcsWebhookDelivery> page = deliveryRepository.findByProjectKeyAndProvider(
                    PROJECT_KEY,
                    VcsProvider.GITHUB,
                    PageRequest.of(
                            0, 2, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));

            // then
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getContent()).extracting(VcsWebhookDelivery::getId).containsExactly(third, second);
            assertThat(first).isNotNull();
        }

        @Test
        @DisplayName("success: another project's deliveries are not listed")
        void scopesToProject() {
            // given
            sut.create(VcsProvider.GITHUB, "d-1", PROJECT_KEY, "push", PAYLOAD);
            sut.create(VcsProvider.GITHUB, "d-2", "OTHER", "push", PAYLOAD);

            // when
            Page<VcsWebhookDelivery> page = deliveryRepository.findByProjectKeyAndProvider(
                    PROJECT_KEY, VcsProvider.GITHUB, PageRequest.of(0, 20));

            // then
            assertThat(page.getContent())
                    .extracting(VcsWebhookDelivery::getProjectKey)
                    .containsOnly(PROJECT_KEY);
        }
    }

    @Nested
    @DisplayName("purge old deliveries")
    class PurgeDeliveries {

        @Test
        @DisplayName("success: deliveries newer than the threshold are kept")
        void keepsRecentDeliveries() {
            // given
            Long id = sut.create(VcsProvider.GITHUB, DELIVERY_ID, PROJECT_KEY, "push", PAYLOAD);

            // when
            int purged = sut.purgeOlderThan(Instant.now().minus(Duration.ofDays(30)));

            // then
            assertThat(purged).isZero();
            assertThat(deliveryRepository.findById(id)).isPresent();
        }
    }
}
