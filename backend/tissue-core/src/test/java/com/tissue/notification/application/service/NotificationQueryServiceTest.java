package com.tissue.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.tissue.feature.notification.application.dto.response.NotificationResponse;
import com.tissue.feature.notification.application.port.repository.NotificationRepository;
import com.tissue.feature.notification.application.service.NotificationQueryService;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.constant.NotificationDataKeys;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.notification.domain.vo.NotificationMessage;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.dto.CursorPageResponse;
import com.tissue.shared.enums.SupportedLanguage;
import com.tissue.shared.vo.EntityReference;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock
    NotificationRepository repository;

    @InjectMocks
    NotificationQueryService sut;

    @Nested
    @DisplayName("get notifications")
    class GetNotifications {

        @Test
        @DisplayName("success: returns mapped responses with raw data")
        void success_GetNotifications() {
            String workspaceKey = "TEST-WS";
            Long memberId = 1L;
            boolean unreadOnly = false;
            Long cursorId = null;
            int limit = 20;

            Notification notification = Notification.builder()
                    .eventId(UUID.randomUUID())
                    .notificationType(NotificationType.ISSUE_CREATED)
                    .entityReference(EntityReference.forIssue(workspaceKey, "TESTPROJ", "TESTPROJ-1"))
                    .actorMemberId(2L)
                    .actorDisplayName("Actor")
                    .receiverMemberId(memberId)
                    .receiverEmail("test@test.com")
                    .receiverLanguage(SupportedLanguage.EN)
                    .message(new NotificationMessage(Map.of(NotificationDataKeys.ISSUE_KEY, "TESTPROJ-1")))
                    .build();

            ReflectionTestUtils.setField(notification, "id", 100L);

            given(repository.findByCursor(
                            ArgumentMatchers.eq(memberId),
                            ArgumentMatchers.eq(workspaceKey),
                            ArgumentMatchers.eq(cursorId),
                            ArgumentMatchers.any(Pageable.class)))
                    .willReturn(List.of(notification));

            WorkspaceMemberContext actor = new WorkspaceMemberContext(
                    1L, memberId, 1L, workspaceKey, "test@test.com", "Gildong", WorkspaceRole.MEMBER);

            CursorPageResponse<NotificationResponse> result = sut.getNotifications(actor, unreadOnly, cursorId, limit);

            assertThat(result.content()).hasSize(1);
            NotificationResponse response = result.content().get(0);
            assertThat(response.type()).isEqualTo(NotificationType.ISSUE_CREATED);
            assertThat(response.data()).containsEntry(NotificationDataKeys.ISSUE_KEY, "TESTPROJ-1");
            assertThat(result.nextCursorId()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("check unread status")
    class CheckUnreadStatus {

        @Test
        @DisplayName("success: returns true if unread notification exists")
        void success_CheckUnreadStatus() {
            String workspaceKey = "TEST-WS";
            Long memberId = 1L;
            given(repository.existsByReceiverMemberIdAndEntityReference_WorkspaceKeyAndIsReadFalse(
                            memberId, workspaceKey))
                    .willReturn(true);

            WorkspaceMemberContext actor = new WorkspaceMemberContext(
                    1L, memberId, 1L, workspaceKey, "test@test.com", "Gildong", WorkspaceRole.MEMBER);

            boolean result = sut.checkUnreadStatus(actor);

            assertThat(result).isTrue();
        }
    }
}
