package com.tissue.notification.adapter.in.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.tissue.global.email.domain.EmailClient;
import com.tissue.member.domain.event.VerificationEmailRequestedEvent;
import com.tissue.notification.adapter.event.VerificationMailListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@ExtendWith(MockitoExtension.class)
class VerificationMailListenerTest {

    @Mock
    EmailClient emailClient;

    @Mock
    SpringTemplateEngine templateEngine;

    @InjectMocks
    VerificationMailListener sut;

    @Test
    @DisplayName("handleVerificationEmailRequest: renders template and sends email")
    void handleVerificationEmailRequest_success() {
        String email = "test@tissue.com";
        String link = "http://localhost:8080/verify?token=abc";
        VerificationEmailRequestedEvent event = VerificationEmailRequestedEvent.create(email, link);

        String renderedContent = "<html>Verify here</html>";
        given(templateEngine.process(eq("mail/verification-email"), any(Context.class)))
            .willReturn(renderedContent);

        sut.handleVerificationEmailRequest(event);

        then(templateEngine).should().process(eq("mail/verification-email"), any(Context.class));
        then(emailClient).should().send(eq(email), any(String.class), eq(renderedContent));
    }

    @Test
    @DisplayName("recover: logs error on failure")
    void recover_logsError() {
        Exception e = new RuntimeException("SMTP Down");
        VerificationEmailRequestedEvent event = VerificationEmailRequestedEvent.create(
            "test@tissue.com", "link");

        sut.recover(e, event);
    }
}
