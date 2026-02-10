package com.tissue.notification.application.listener;

import static org.mockito.BDDMockito.then;

import com.tissue.member.domain.event.VerificationEmailRequestedEvent;
import com.tissue.notification.application.port.in.SendVerificationEmailUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationMailListenerTest {

    @Mock
    SendVerificationEmailUseCase verificationEmailUseCase;

    @InjectMocks
    VerificationMailListener sut;

    @Test
    @DisplayName("handleVerificationEmailRequest: delegates to UseCase")
    void handleVerificationEmailRequest_success() {
        // given
        String email = "test@tissue.com";
        String link = "http://localhost:8080/verify?token=abc";
        VerificationEmailRequestedEvent event = VerificationEmailRequestedEvent.create(email, link);

        // when
        sut.handleVerificationEmailRequest(event);

        // then
        then(verificationEmailUseCase).should().sendVerificationEmail(event);
    }

    @Test
    @DisplayName("recover: logs error on failure")
    void recover_logsError() {
        Exception e = new RuntimeException("SMTP Down");
        VerificationEmailRequestedEvent event = VerificationEmailRequestedEvent.create("test@tissue.com", "link");

        sut.recover(e, event);
    }
}
