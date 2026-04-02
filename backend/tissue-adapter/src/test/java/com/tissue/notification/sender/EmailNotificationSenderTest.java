package com.tissue.notification.sender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import com.tissue.feature.notification.application.port.email.EmailClient;
import com.tissue.feature.notification.application.port.repository.FailedEmailRepository;
import com.tissue.feature.notification.application.port.repository.NotificationTemplateRenderer;
import com.tissue.feature.notification.domain.FailedEmail;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.notification.domain.vo.NotificationMessage;
import com.tissue.feature.notification.sender.EmailNotificationSender;
import com.tissue.shared.enums.SupportedLanguage;
import com.tissue.shared.vo.EntityReference;
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
            // given
            Notification notification = Notification.create(
                    UUID.randomUUID(),
                    NotificationType.ISSUE_CREATED,
                    EntityReference.forIssue("TESTWS", "TESTPROJ", "TESTPROJ-1"),
                    1L,
                    "test@test.com",
                    SupportedLanguage.EN,
                    new NotificationMessage(Map.of("key", "value")),
                    2L,
                    "Actor Name");

            given(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                    .willReturn("Template");

            given(templateRenderer.renderString(anyString(), any())).willReturn("Rendered String");
            given(templateRenderer.renderHtml(anyString(), any())).willReturn("<html>Body</html>");

            // when
            sut.send(notification);

            // then
            then(emailClient).should().send(eq("test@test.com"), eq("Rendered String"), eq("<html>Body</html>"));
            then(templateRenderer).should().renderHtml(eq("mail/notification-email"), any());
        }

        @Test
        @DisplayName("fail: saves FailedEmail on exception")
        void fail_SavesFailedEmail() {
            // given
            Notification notification = Notification.create(
                    UUID.randomUUID(),
                    NotificationType.ISSUE_CREATED,
                    EntityReference.forIssue("TESTWS", "TESTPROJ", "TESTPROJ-1"),
                    1L,
                    "test@test.com",
                    SupportedLanguage.EN,
                    new NotificationMessage(Map.of()),
                    2L,
                    "Actor Name");

            given(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                    .willReturn("Template");

            given(templateRenderer.renderString(anyString(), any())).willReturn("String");
            given(templateRenderer.renderHtml(anyString(), any())).willReturn("HTML");

            doThrow(new RuntimeException("Fail")).when(emailClient).send(anyString(), anyString(), anyString());

            // when
            sut.send(notification);

            // then
            then(failedEmailRepository).should().save(any(FailedEmail.class));
        }
    }
}
