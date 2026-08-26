package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.tissue.security.application.port.repository.RateLimitStore;
import com.tissue.security.application.service.RateLimitService;
import com.tissue.security.config.RateLimitProperties;
import com.tissue.shared.exception.base.RateLimitExceededException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceWebhookTest {

    @Mock
    private RateLimitStore rateLimitStore;

    private RateLimitProperties properties;
    private RateLimitService sut;

    private static final String CLIENT_IP = "140.82.115.1";

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        sut = new RateLimitService(rateLimitStore, properties);
    }

    @Test
    @DisplayName("success: traffic within the window budget is allowed")
    void allowsTrafficWithinBudget() {
        // given
        given(rateLimitStore.incrementAndGet(
                        "rate:vcs-webhook:" + CLIENT_IP, properties.getWebhook().getWindow()))
                .willReturn(properties.getWebhook().getMaxAttempts());

        // when & then
        assertThatCode(() -> sut.checkWebhookRateLimit(CLIENT_IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("fail: traffic past the window budget is rejected")
    void rejectsTrafficPastBudget() {
        // given
        given(rateLimitStore.incrementAndGet(
                        "rate:vcs-webhook:" + CLIENT_IP, properties.getWebhook().getWindow()))
                .willReturn(properties.getWebhook().getMaxAttempts() + 1);

        // when & then
        assertThatThrownBy(() -> sut.checkWebhookRateLimit(CLIENT_IP)).isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("success: the default budget is well above real webhook traffic")
    void defaultBudgetLeavesHeadroom() {
        // then
        assertThat(properties.getWebhook().getMaxAttempts()).isGreaterThanOrEqualTo(60);
        assertThat(properties.getWebhook().getWindow()).isEqualTo(Duration.ofMinutes(1));
    }
}
