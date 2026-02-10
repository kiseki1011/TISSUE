package com.tissue.notification.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.notification.application.port.out.NotificationRepository;
import com.tissue.feature.notification.application.service.NotificationCommandService;
import com.tissue.feature.notification.application.service.NotificationProcessor;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.notification.domain.service.NotificationMessageFactory;
import com.tissue.feature.notification.domain.vo.NotificationMessage;
import com.tissue.feature.workspace.application.port.out.WorkspaceMemberContactInfo;
import com.tissue.shared.enums.SupportedLanguage;
import com.tissue.shared.exception.base.ForbiddenException;
import com.tissue.shared.exception.base.ResourceNotFoundException;
import com.tissue.shared.vo.EntityReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @Mock
    NotificationRepository repository;

    @Mock
    NotificationMessageFactory messageFactory;

    @Mock
    NotificationProcessor processor;

    @InjectMocks
    NotificationCommandService sut;

    @Nested
    @DisplayName("create and send")
    class CreateAndSend {

        @Test
        @DisplayName("success: saves notifications and triggers process of notification processor")
        void success_CreateAndSend() {
            UUID eventId = UUID.randomUUID();
            NotificationType type = NotificationType.ISSUE_CREATED;
            EntityReference ref = EntityReference.forIssue("TESTWS", "TESTPROJ", "TESTPROJ-1");

            WorkspaceMemberContactInfo contact = mock(WorkspaceMemberContactInfo.class);
            given(contact.getMemberId()).willReturn(10L);
            given(contact.getEmail()).willReturn("test@test.com");
            given(contact.getLanguage()).willReturn(SupportedLanguage.EN);

            List<WorkspaceMemberContactInfo> receivers = List.of(contact);
            Long actorId = 1L;
            String actorName = "Actor";
            Map<String, String> data = Map.of("key", "value");

            given(messageFactory.createMessage(type, data)).willReturn(new NotificationMessage(data));

            sut.createAndSend(eventId, type, ref, receivers, actorId, actorName, data);

            then(repository).should().saveAll(anyList());
            then(processor).should().process(anyList());
        }

        @Test
        @DisplayName("success: does nothing if receivers empty")
        void success_NoReceivers() {
            List<WorkspaceMemberContactInfo> receivers = Collections.emptyList();

            sut.createAndSend(
                    UUID.randomUUID(), NotificationType.ISSUE_CREATED, null, receivers, 1L, "Actor", Map.of());

            then(repository).shouldHaveNoInteractions();
            then(processor).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("read notification")
    class ReadNotification {

        @Test
        @DisplayName("success: marks notification as read")
        void success_ReadNotification() {
            Long notificationId = 100L;
            Long memberId = 1L;
            Notification notification = mock(Notification.class);

            given(notification.getReceiverMemberId()).willReturn(memberId);
            given(repository.findById(notificationId)).willReturn(Optional.of(notification));

            sut.readNotification(notificationId, memberId);

            then(notification).should().markAsRead();
            then(repository).should().save(notification);
        }

        @Test
        @DisplayName("fail: notification not found")
        void fail_NotFound() {
            Long notificationId = 100L;
            Long memberId = 1L;
            given(repository.findById(notificationId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.readNotification(notificationId, memberId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("fail: cannot read other workspace member's notification")
        void fail_Forbidden() {
            Long notificationId = 100L;
            Long memberId = 1L;
            Long otherMemberId = 2L;
            Notification notification = mock(Notification.class);

            given(notification.getReceiverMemberId()).willReturn(otherMemberId);
            given(repository.findById(notificationId)).willReturn(Optional.of(notification));

            assertThatThrownBy(() -> sut.readNotification(notificationId, memberId))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("read all notifications")
    class ReadAllNotifications {

        @Test
        @DisplayName("success: marks all notifications as read")
        void success_ReadAllNotifications() {
            String workspaceKey = "TESTWS";
            Long memberId = 1L;

            sut.readAllNotifications(workspaceKey, memberId);

            then(repository).should().markAllAsRead(memberId, workspaceKey);
        }
    }
}
