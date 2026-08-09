package com.tissue.feature.vcs.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.tissue.feature.vcs.domain.enums.VcsProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class VcsWebhookInboxServiceTest {

    @Mock
    private VcsWebhookDeliveryWriter writer;

    @Mock
    private VcsWebhookProcessor processor;

    @InjectMocks
    private VcsWebhookInboxService sut;

    private static final String DELIVERY_ID = "b1a2c3";
    private static final String PROJECT_KEY = "PROJ";
    private static final String PAYLOAD = "{}";

    @Test
    @DisplayName("success: a new delivery is stored and queued for processing")
    void storesAndQueuesNewDelivery() {
        // given
        given(writer.create(VcsProvider.GITHUB, DELIVERY_ID, PROJECT_KEY, "push", PAYLOAD))
                .willReturn(42L);

        // when
        boolean accepted = sut.receive(VcsProvider.GITHUB, DELIVERY_ID, PROJECT_KEY, "push", PAYLOAD);

        // then
        assertThat(accepted).isTrue();
        then(processor).should().processAsync(42L);
    }

    @Test
    @DisplayName("ignore: a redelivered event is dropped instead of replayed")
    void dropsDuplicateDelivery() {
        // given
        willThrow(new DataIntegrityViolationException("duplicate key"))
                .given(writer)
                .create(any(VcsProvider.class), anyString(), anyString(), anyString(), anyString());

        // when
        boolean accepted = sut.receive(VcsProvider.GITHUB, DELIVERY_ID, PROJECT_KEY, "push", PAYLOAD);

        // then
        assertThat(accepted).isFalse();
        then(processor).should(never()).processAsync(any());
    }
}
