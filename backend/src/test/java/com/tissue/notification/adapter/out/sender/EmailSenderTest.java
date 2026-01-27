package com.tissue.notification.adapter.out.sender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import com.tissue.common.enums.SupportedLanguage;
import com.tissue.common.vo.EntityReference;
import com.tissue.email.domain.EmailClient;
import com.tissue.notification.application.port.out.FailedEmailRepository;
import com.tissue.notification.domain.FailedEmail;
import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.service.NotificationTemplateRenderer;
import com.tissue.notification.domain.vo.NotificationMessage;
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
class EmailSenderTest {

    @Mock
    EmailClient emailClient;

    @Mock
    MessageSource messageSource;

    @Mock
    FailedEmailRepository failedEmailRepository;

    @Mock
    NotificationTemplateRenderer templateRenderer;

    @InjectMocks
    EmailSender sut;

    @Nested
    @DisplayName("send email")
    class Send {
        @Test
        @DisplayName("success: sends email")
        void success_Send() {
            Notification notification = Notification.builder()
                    .eventId(UUID.randomUUID())
                    .notificationType(NotificationType.ISSUE_CREATED)
                    .entityReference(EntityReference.forIssue("TESTWS", "TESTPROJ", "TESTPROJ-1", 1L))
                    .actorMemberId(2L)
                    .receiverMemberId(1L)
                    .receiverEmail("test@test.com")
                    .receiverLanguage(SupportedLanguage.EN)
                    .message(new NotificationMessage(Map.of("key", "value")))
                    .build();

            given(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                    .willReturn("Template with {key}");

            given(templateRenderer.renderString(anyString(), any())).willReturn("Template with value");

            sut.send(notification);

            then(emailClient).should().send(eq("test@test.com"), anyString(), eq("Template with value"));
        }

        @Test
        @DisplayName("fail: saves FailedEmail on exception")
        void fail_SavesFailedEmail() {
            Notification notification = Notification.builder()
                    .eventId(UUID.randomUUID())
                    .notificationType(NotificationType.ISSUE_CREATED)
                    .entityReference(EntityReference.forIssue("TESTWS", "TESTPROJ", "TESTPROJ-1", 1L))
                    .actorMemberId(2L)
                    .receiverMemberId(1L)
                    .receiverEmail("test@test.com")
                    .receiverLanguage(SupportedLanguage.EN)
                    .message(new NotificationMessage(Map.of()))
                    .build();

            given(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                    .willReturn("Template");

            doThrow(new RuntimeException("Fail")).when(emailClient).send(anyString(), anyString(), anyString());

            sut.send(notification);

            then(failedEmailRepository).should().save(any(FailedEmail.class));
        }
    }
}
