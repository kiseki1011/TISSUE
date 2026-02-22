package com.tissue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.tissue.application.port.repository.EmailVerificationRepository;
import com.tissue.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.application.service.MemberEmailVerificationService;
import com.tissue.feature.member.config.EmailVerificationProperties;
import com.tissue.feature.member.domain.event.VerificationEmailRequestedEvent;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
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

    @InjectMocks
    MemberEmailVerificationService sut;

    @Test
    @DisplayName("sendSignupVerificationEmail: generates token, saves it, and publishes event")
    void sendSignupVerificationEmail_success() {
        String email = "test@tissue.com";
        Duration ttl = Duration.ofMinutes(30);
        given(properties.getVerificationEmailTtl()).willReturn(ttl);
        given(properties.getBaseUrl()).willReturn("http://localhost:8080");

        String result = sut.sendSignupVerificationEmail(email);

        assertThat(result).isNotNull();
        then(repository).should().storeVerificationContext(eq(result), eq(email), anyString(), eq(ttl));
        then(eventPublisher).should().publishEvent(any(VerificationEmailRequestedEvent.class));
    }

    @Test
    @DisplayName("verifyEmail: delegates to repository")
    void verifyEmail() {
        String token = "token";
        Duration ttl = Duration.ofMinutes(10);
        given(properties.getVerifiedTokenTtl()).willReturn(ttl);
        given(repository.verifyByEmailToken(token, ttl)).willReturn(true);

        boolean result = sut.verifyEmail(token);

        assertThat(result).isTrue();
        then(repository).should().verifyByEmailToken(token, ttl);
    }

    @Test
    @DisplayName("getVerificationStatus: delegates to repository")
    void getVerificationStatus() {
        String verficationId = "test-v-id";
        VerificationStatus status = new VerificationStatus("VERIFIED", "signup-token");
        given(repository.getStatus(verficationId)).willReturn(status);

        VerificationStatus result = sut.getVerificationStatus(verficationId);

        assertThat(result).isEqualTo(status);
        then(repository).should().getStatus(verficationId);
    }

    @Test
    @DisplayName("isTokenVerified: delegates to repository")
    void isTokenVerified() {
        String email = "test@tissue.com";
        String signupToken = "s-token";
        given(repository.validateVerifiedToken(signupToken)).willReturn(email);

        boolean result = sut.isTokenVerified(email, signupToken);

        assertThat(result).isTrue();
        then(repository).should().validateVerifiedToken(signupToken);
    }
}
