package com.tissue.feature.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.notification.application.dto.response.NotificationResponse;
import com.tissue.feature.notification.application.port.repository.NotificationRepository;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.constant.NotificationDataKeys;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.notification.domain.vo.NotificationMessage;
import com.tissue.shared.dto.CursorPageResponse;
import com.tissue.shared.vo.EntityReference;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

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
        @DisplayName("success: returns mapped responses from findByCursor when unreadOnly is false")
        void success_AllNotifications() {
            // given
            String workspaceKey = "WORKSPACE";
            Long memberId = 1L;

            Notification notification = createMockNotification(100L, workspaceKey);

            given(repository.findByCursor(eq(memberId), eq(workspaceKey), eq(null), any(Pageable.class)))
                    .willReturn(List.of(notification));

            // when
            CursorPageResponse<NotificationResponse> result =
                    sut.getNotifications(workspaceKey, memberId, false, null, 20);

            // then
            assertThat(result.content()).hasSize(1);
            NotificationResponse response = result.content().getFirst();
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.type()).isEqualTo(NotificationType.ISSUE_CREATED);
            assertThat(response.data()).containsEntry(NotificationDataKeys.ISSUE_KEY, "PROJ-1");
            assertThat(result.nextCursorId()).isNotNull();
            assertThat(result.hasNext()).isTrue();
        }

        @Test
        @DisplayName("success: returns mapped responses from findUnreadByCursor when unreadOnly is true")
        void success_UnreadOnly() {
            // given
            String workspaceKey = "WORKSPACE";
            Long memberId = 1L;

            Notification notification = createMockNotification(200L, workspaceKey);

            given(repository.findUnreadByCursor(eq(memberId), eq(workspaceKey), eq(null), any(Pageable.class)))
                    .willReturn(List.of(notification));

            // when
            CursorPageResponse<NotificationResponse> result =
                    sut.getNotifications(workspaceKey, memberId, true, null, 20);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().id()).isEqualTo(200L);
            assertThat(result.nextCursorId()).isNotNull();
        }

        @Test
        @DisplayName("success: returns empty result with null cursor when no notifications")
        void success_EmptyResult() {
            // given
            String workspaceKey = "WORKSPACE";
            Long memberId = 1L;

            given(repository.findByCursor(eq(memberId), eq(workspaceKey), eq(null), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            CursorPageResponse<NotificationResponse> result =
                    sut.getNotifications(workspaceKey, memberId, false, null, 20);

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.nextCursorId()).isNull();
            assertThat(result.hasNext()).isFalse();
        }
    }

    private Notification createMockNotification(Long id, String workspaceKey) {
        Notification notification = mock(Notification.class);
        given(notification.getId()).willReturn(id);
        given(notification.getEventId()).willReturn(UUID.randomUUID());
        given(notification.getNotificationType()).willReturn(NotificationType.ISSUE_CREATED);
        given(notification.getMessage())
                .willReturn(new NotificationMessage(Map.of(NotificationDataKeys.ISSUE_KEY, "PROJ-1")));
        given(notification.getEntityReference()).willReturn(EntityReference.forIssue(workspaceKey, "PROJ", "PROJ-1"));
        given(notification.getActorMemberId()).willReturn(2L);
        given(notification.getActorDisplayName()).willReturn("actor");
        given(notification.isRead()).willReturn(false);
        given(notification.getCreatedAt()).willReturn(Instant.now());
        return notification;
    }
}
