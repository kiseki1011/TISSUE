package com.tissue.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.tissue.feature.member.application.port.out.EmailVerificationRepository;
import com.tissue.feature.member.application.port.out.EmailVerificationRepository.VerificationStatus;
import com.tissue.feature.member.application.service.MemberEmailVerificationService;
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
    @DisplayName("sendVerificationEmail: generates token, saves it, and publishes event")
    void sendVerificationEmail_success() {
        String email = "test@tissue.com";
        String verificationId = "test-v-id";
        given(properties.getTtl()).willReturn(Duration.ofMinutes(30));
        given(properties.getVerificationUrl()).willReturn("http://localhost:8080/verify");
        given(repository.startVerification(eq(email), anyString(), any(Duration.class)))
                .willReturn(verificationId);

        String result = sut.sendVerificationEmail(email);

        assertThat(result).isEqualTo(verificationId);
        then(repository).should().startVerification(eq(email), anyString(), eq(Duration.ofMinutes(30)));
        then(eventPublisher).should().publishEvent(any(VerificationEmailRequestedEvent.class));
    }

    @Test
    @DisplayName("verifyEmail: delegates to repository")
    void verifyEmail() {
        String token = "token";
        given(repository.verifyByToken(token)).willReturn(true);

        boolean result = sut.verifyEmail(token);

        assertThat(result).isTrue();
        then(repository).should().verifyByToken(token);
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
    @DisplayName("validateSignupToken: delegates to repository")
    void validateSignupToken() {
        String email = "test@tissue.com";
        String signupToken = "s-token";
        given(repository.validateSignupToken(email, signupToken)).willReturn(true);

        boolean result = sut.validateSignupToken(email, signupToken);

        assertThat(result).isTrue();
        then(repository).should().validateSignupToken(email, signupToken);
    }
}
