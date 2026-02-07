package com.tissue.tissue.notification.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tissue.common.enums.SupportedLanguage;
import com.tissue.global.vo.EntityReference;
import com.tissue.notification.application.port.out.NotificationPreferenceRepository;
import com.tissue.notification.application.service.NotificationProcessor;
import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.NotificationPreference;
import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.service.NotificationSender;
import com.tissue.notification.domain.vo.NotificationMessage;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationProcessorTest {

    @Mock
    NotificationSender emailSender;

    @Mock
    NotificationPreferenceRepository preferenceRepository;

    @Mock
    Executor emailExecutor;

    NotificationProcessor sut;

    @BeforeEach
    void setUp() {
        given(emailSender.getChannel()).willReturn(NotificationChannel.EMAIL);
        sut = new NotificationProcessor(List.of(emailSender), preferenceRepository, emailExecutor);
    }

    @Nested
    @DisplayName("process notifications")
    class ProcessNotification {

        @Test
        @DisplayName("success: sends email if enabled in preference (default true)")
        void success_Process() {
            sut = new NotificationProcessor(List.of(emailSender), preferenceRepository, Runnable::run);

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

            given(preferenceRepository.findAllByWorkspaceKeyAndReceiverMemberIdIn("TESTWS", List.of(1L)))
                    .willReturn(Collections.emptyList());

            sut.process(List.of(notification));

            then(emailSender).should().send(notification);
        }

        @Test
        @DisplayName("success: does not send if disabled in preference")
        void success_SkipIfDisabled() {
            sut = new NotificationProcessor(List.of(emailSender), preferenceRepository, Runnable::run);

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

            NotificationPreference pref = NotificationPreference.builder()
                    .workspaceKey("TESTWS")
                    .receiverMemberId(1L)
                    .build();
            pref.updatePreference(NotificationChannel.EMAIL, NotificationType.ISSUE_CREATED, false);

            given(preferenceRepository.findAllByWorkspaceKeyAndReceiverMemberIdIn("TESTWS", List.of(1L)))
                    .willReturn(List.of(pref));

            sut.process(List.of(notification));

            then(emailSender).should(never()).send(any());
        }
    }
}
