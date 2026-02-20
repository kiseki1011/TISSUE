package com.tissue.notification.sender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import com.tissue.feature.notification.application.port.repository.FailedEmailRepository;
import com.tissue.feature.notification.application.port.repository.NotificationTemplateRenderer;
import com.tissue.feature.notification.domain.FailedEmail;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.notification.domain.vo.NotificationMessage;
import com.tissue.feature.notification.sender.EmailNotificationSender;
import com.tissue.shared.enums.SupportedLanguage;
import com.tissue.shared.vo.EntityReference;
import com.tissue.support.email.EmailClient;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    @Mock
    EmailClient emailClient;

    @Mock
    MessageSource messageSource;

    @Mock
    FailedEmailRepository failedEmailRepository;

    @Mock
    NotificationTemplateRenderer templateRenderer;

    @InjectMocks
    EmailNotificationSender sut;

    @Nested
    @DisplayName("send email")
    class Send {

        @Test
        @DisplayName("success: sends email with HTML body")
        void success_Send() {
            Notification notification = Notification.builder()
                    .eventId(UUID.randomUUID())
                    .notificationType(NotificationType.ISSUE_CREATED)
                    .entityReference(EntityReference.forIssue("TESTWS", "TESTPROJ", "TESTPROJ-1"))
                    .actorMemberId(2L)
                    .receiverMemberId(1L)
                    .receiverEmail("test@test.com")
                    .receiverLanguage(SupportedLanguage.EN)
                    .message(new NotificationMessage(Map.of("key", "value")))
                    .build();

            given(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                    .willReturn("Template");

            // Mock renderString for title and content
            given(templateRenderer.renderString(anyString(), any())).willReturn("Rendered String");

            // Mock renderHtml for email body
            given(templateRenderer.renderHtml(anyString(), any())).willReturn("<html>Body</html>");

            sut.send(notification);

            // Verify email sent with HTML body
            then(emailClient).should().send(eq("test@test.com"), eq("Rendered String"), eq("<html>Body</html>"));

            // Verify interactions
            then(templateRenderer).should().renderHtml(eq("mail/notification-email"), any());
        }

        @Test
        @DisplayName("fail: saves FailedEmail on exception")
        void fail_SavesFailedEmail() {
            Notification notification = Notification.builder()
                    .eventId(UUID.randomUUID())
                    .notificationType(NotificationType.ISSUE_CREATED)
                    .entityReference(EntityReference.forIssue("TESTWS", "TESTPROJ", "TESTPROJ-1"))
                    .actorMemberId(2L)
                    .receiverMemberId(1L)
                    .receiverEmail("test@test.com")
                    .receiverLanguage(SupportedLanguage.EN)
                    .message(new NotificationMessage(Map.of()))
                    .build();

            given(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                    .willReturn("Template");

            given(templateRenderer.renderString(anyString(), any())).willReturn("String");
            given(templateRenderer.renderHtml(anyString(), any())).willReturn("HTML");

            doThrow(new RuntimeException("Fail")).when(emailClient).send(anyString(), anyString(), anyString());

            sut.send(notification);

            then(failedEmailRepository).should().save(any(FailedEmail.class));
        }
    }
}
