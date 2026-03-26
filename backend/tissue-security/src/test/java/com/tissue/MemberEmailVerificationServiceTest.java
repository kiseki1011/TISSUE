package com.tissue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.tissue.feature.member.domain.event.VerificationEmailRequestedEvent;
import com.tissue.security.application.port.repository.EmailVerificationRepository;
import com.tissue.security.application.service.MemberEmailVerificationService;
import com.tissue.security.application.service.RateLimitService;
import com.tissue.security.config.EmailVerificationProperties;
import com.tissue.shared.exception.CommonErrorCode;
import com.tissue.shared.exception.base.RateLimitExceededException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MemberEmailVerificationServiceTest {

    @Mock
    EmailVerificationProperties properties;

    @Mock
    EmailVerificationRepository repository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    RateLimitService rateLimitService;

    @InjectMocks
    MemberEmailVerificationService sut;

    @Nested
    @DisplayName("send signup verification email")
    class SendSignupVerificationEmail {

        @Test
        @DisplayName("success: generates token, saves it, and publishes event with signup path")
        void success() {
            // given
            String email = "test@tissue.com";
            Duration ttl = Duration.ofMinutes(30);
            given(properties.getEmailTtl()).willReturn(ttl);
            given(properties.getBaseUrl()).willReturn("http://localhost:8080");
            given(properties.getSignupVerifyPath()).willReturn("/api/v1/members/signup/verify");

            // when
            String result = sut.sendSignupVerificationEmail(email);

            // then
            assertThat(result).isNotNull();
            then(rateLimitService).should().checkEmailVerificationRateLimit(email);
            then(repository).should().storeVerificationContext(eq(result), eq(email), anyString(), eq(ttl));
            then(eventPublisher).should().publishEvent(any(VerificationEmailRequestedEvent.class));
        }

        @Test
        @DisplayName("fail: throws exception when rate limit exceeded")
        void fail_RateLimitExceeded() {
            // given
            String email = "test@tissue.com";
            willThrow(new RateLimitExceededException(CommonErrorCode.RATE_LIMITED))
                    .given(rateLimitService)
                    .checkEmailVerificationRateLimit(email);

            // when & then
            assertThatThrownBy(() -> sut.sendSignupVerificationEmail(email))
                    .isInstanceOf(RateLimitExceededException.class);

            then(repository).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("send password reset verification email")
    class SendPasswordResetVerificationEmail {

        @Test
        @DisplayName("success: generates and saves token, then publishes event")
        void success() {
            // given
            String email = "test@tissue.com";
            Duration ttl = Duration.ofMinutes(30);
            given(properties.getEmailTtl()).willReturn(ttl);
            given(properties.getBaseUrl()).willReturn("http://localhost:8080");
            given(properties.getPasswordResetVerifyPath()).willReturn("/api/v1/members/password/verify");

            // when
            String result = sut.sendPasswordResetVerificationEmail(email);

            // then
            assertThat(result).isNotNull();
            then(repository).should().storeVerificationContext(eq(result), eq(email), anyString(), eq(ttl));
            then(eventPublisher).should().publishEvent(any(VerificationEmailRequestedEvent.class));
        }
    }
}
