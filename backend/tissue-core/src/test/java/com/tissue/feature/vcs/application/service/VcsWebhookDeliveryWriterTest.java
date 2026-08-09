package com.tissue.feature.vcs.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.tissue.feature.vcs.application.dto.VcsEventResult;
import com.tissue.feature.vcs.application.port.repository.VcsWebhookDeliveryRepository;
import com.tissue.feature.vcs.application.port.usecase.VcsWebhookDispatcher;
import com.tissue.feature.vcs.config.VcsWebhookProperties;
import com.tissue.feature.vcs.domain.VcsWebhookDelivery;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.enums.WebhookDeliveryStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VcsWebhookDeliveryWriterTest {

    @Mock
    private VcsWebhookDeliveryRepository repository;

    @Mock
    private VcsWebhookDispatcher dispatcher;

    private VcsWebhookProperties properties;
    private VcsWebhookDeliveryWriter sut;

    private static final Long DELIVERY_PK = 1L;

    @BeforeEach
    void setUp() {
        properties = new VcsWebhookProperties();
        given(dispatcher.provider()).willReturn(VcsProvider.GITHUB);
        sut = new VcsWebhookDeliveryWriter(repository, properties, List.of(dispatcher));
    }

    @Test
    @DisplayName("success: a handled event marks the delivery processed")
    void marksProcessedWhenHandled() {
        // given
        VcsWebhookDelivery delivery = delivery();
        given(repository.findById(DELIVERY_PK)).willReturn(Optional.of(delivery));
        given(dispatcher.dispatch("PROJ", "push", "{}")).willReturn(VcsEventResult.handled("Linked branch"));

        // when
        sut.attempt(DELIVERY_PK);

        // then
        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.PROCESSED);
        assertThat(delivery.getResultDetail()).isEqualTo("Linked branch");
    }

    @Test
    @DisplayName("success: a deliberately skipped event marks the delivery ignored, not failed")
    void marksIgnoredWhenSkipped() {
        // given
        VcsWebhookDelivery delivery = delivery();
        given(repository.findById(DELIVERY_PK)).willReturn(Optional.of(delivery));
        given(dispatcher.dispatch("PROJ", "push", "{}"))
                .willReturn(VcsEventResult.skipped("No issue key found in: main"));

        // when
        sut.attempt(DELIVERY_PK);

        // then
        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.IGNORED);
        assertThat(delivery.getResultDetail()).isEqualTo("No issue key found in: main");
    }

    @Test
    @DisplayName("success: a failure below the retry budget schedules another attempt")
    void schedulesRetryWhenBudgetRemains() {
        // given
        VcsWebhookDelivery delivery = delivery();
        given(repository.findById(DELIVERY_PK)).willReturn(Optional.of(delivery));

        // when
        sut.recordFailure(DELIVERY_PK, new IllegalStateException("db down"));

        // then
        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.FAILED);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getNextAttemptAt()).isNotNull();
        assertThat(delivery.getLastError()).contains("db down");
    }

    @Test
    @DisplayName("success: exhausting the retry budget parks the delivery as dead")
    void parksDeliveryWhenBudgetExhausted() {
        // given
        VcsWebhookDelivery delivery = delivery();
        given(repository.findById(DELIVERY_PK)).willReturn(Optional.of(delivery));

        // when
        for (int i = 0; i < properties.getMaxAttempts(); i++) {
            sut.recordFailure(DELIVERY_PK, new IllegalStateException("db down"));
        }

        // then
        assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.DEAD);
        assertThat(delivery.getAttemptCount()).isEqualTo(properties.getMaxAttempts());
        assertThat(delivery.getNextAttemptAt()).isNull();
    }

    @Test
    @DisplayName("ignore: a delivery that already reached a terminal state is not dispatched again")
    void skipsTerminalDelivery() {
        // given
        VcsWebhookDelivery delivery = delivery();
        delivery.markProcessed("already done");
        given(repository.findById(DELIVERY_PK)).willReturn(Optional.of(delivery));

        // when
        sut.attempt(DELIVERY_PK);

        // then
        assertThat(delivery.getResultDetail()).isEqualTo("already done");
    }

    private VcsWebhookDelivery delivery() {
        return VcsWebhookDelivery.create(VcsProvider.GITHUB, "b1a2c3", "PROJ", "push", "{}");
    }
}
