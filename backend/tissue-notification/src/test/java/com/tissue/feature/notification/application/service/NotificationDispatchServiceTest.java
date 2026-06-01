package com.tissue.feature.notification.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tissue.feature.notification.application.port.repository.NotificationPreferenceRepository;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.NotificationPreference;
import com.tissue.feature.notification.domain.enums.NotificationChannel;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.notification.domain.service.NotificationSender;
import com.tissue.feature.notification.domain.vo.NotificationMessage;
import com.tissue.shared.enums.SupportedLanguage;
import com.tissue.shared.vo.EntityReference;
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
class NotificationDispatchServiceTest {

    @Mock
    NotificationSender emailSender;

    @Mock
    NotificationPreferenceRepository preferenceRepository;

    NotificationDispatchService sut;

    @BeforeEach
    void setUp() {
        sut = new NotificationDispatchService(List.of(emailSender), preferenceRepository);
    }

    @Nested
    @DisplayName("dispatch notifications")
    class DispatchNotification {

        @Test
        @DisplayName("success: sends email if enabled in preference (default true)")
        void successDispatch() {
            // given
            // mock executor to run tasks immediately in same thread for testing
            Executor executor = Runnable::run;
            given(emailSender.getChannel()).willReturn(NotificationChannel.EMAIL);
            given(emailSender.getExecutor()).willReturn(executor);

            Notification notification = Notification.create(
                    UUID.randomUUID(),
                    NotificationType.ISSUE_ASSIGNED,
                    EntityReference.forIssue("PROJ", "PROJ-1"),
                    1L,
                    "test@tissue.com",
                    SupportedLanguage.EN,
                    new NotificationMessage(Map.of()),
                    2L,
                    "actor");

            given(preferenceRepository.findAllByReceiverMemberIdIn(List.of(1L))).willReturn(Collections.emptyList());

            // when
            sut.dispatch(List.of(notification));

            // then
            then(emailSender).should().send(notification);
        }

        @Test
        @DisplayName("success: does not send if disabled in preference")
        void successDispatch_Skip_If_Disabled() {
            // given
            given(emailSender.getChannel()).willReturn(NotificationChannel.EMAIL);

            Notification notification = Notification.create(
                    UUID.randomUUID(),
                    NotificationType.ISSUE_ASSIGNED,
                    EntityReference.forIssue("PROJ", "PROJ-1"),
                    1L,
                    "test@tissue.com",
                    SupportedLanguage.EN,
                    new NotificationMessage(Map.of()),
                    2L,
                    "actor");

            NotificationPreference pref =
                    NotificationPreference.builder().receiverMemberId(1L).build();
            pref.updatePreference(NotificationChannel.EMAIL, NotificationType.ISSUE_ASSIGNED, false);

            given(preferenceRepository.findAllByReceiverMemberIdIn(List.of(1L))).willReturn(List.of(pref));

            // when
            sut.dispatch(List.of(notification));

            // then
            then(emailSender).should(never()).getExecutor();
            then(emailSender).should(never()).send(any());
        }
    }
}
