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
import com.tissue.shared.dto.Cursor;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.IdCursor;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
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

@LLMGenerated(
        llmInvolvement = LLMInvolvement.ASSISTED,
        model = "claude-opus-4-8",
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "Reviewed code",
        reviewedBy = "kiseki1011")
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
        @DisplayName("success: returns mapped responses from findByKeyset when unreadOnly is false")
        void success_AllNotifications() {
            // given
            Long memberId = 1L;
            Notification notification = createMockNotification(100L);

            given(repository.findByKeyset(eq(memberId), eq(null), any(Pageable.class)))
                    .willReturn(List.of(notification));

            // when
            CursorPage<NotificationResponse> result = sut.getNotifications(memberId, false, null, null, 20);

            // then
            assertThat(result.content()).hasSize(1);
            NotificationResponse response = result.content().getFirst();
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.type()).isEqualTo(NotificationType.ISSUE_ASSIGNED);
            assertThat(response.data()).containsEntry(NotificationDataKeys.ISSUE_KEY, "PROJ-1");
            // single row under 20 -> no further page
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        @DisplayName("success: routes to findUnreadByKeyset when unreadOnly is true")
        void success_UnreadOnly() {
            // given
            Long memberId = 1L;
            Notification notification = createMockNotification(200L);

            given(repository.findUnreadByKeyset(eq(memberId), eq(null), any(Pageable.class)))
                    .willReturn(List.of(notification));

            // when
            CursorPage<NotificationResponse> result = sut.getNotifications(memberId, true, null, null, 20);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().id()).isEqualTo(200L);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("success: routes to findByKeysetAndTypes when a types filter is present")
        void success_TypesFilter() {
            // given
            Long memberId = 1L;
            List<NotificationType> types = List.of(NotificationType.ISSUE_MENTIONED);
            Notification notification = createMockNotification(300L);

            given(repository.findByKeysetAndTypes(eq(memberId), eq(false), any(), eq(null), any(Pageable.class)))
                    .willReturn(List.of(notification));

            // when
            CursorPage<NotificationResponse> result = sut.getNotifications(memberId, false, types, null, 20);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().id()).isEqualTo(300L);
        }

        @Test
        @DisplayName("success: hasNext + opaque nextCursor when an extra row is fetched (limit+1 look-ahead)")
        void success_HasNext() {
            // given - limit 1, repo returns 2 rows (the extra row signals a next page)
            Long memberId = 1L;
            Notification first = createMockNotification(100L);
            Notification extra = mock(Notification.class);

            given(repository.findByKeyset(eq(memberId), eq(null), any(Pageable.class)))
                    .willReturn(List.of(first, extra));

            // when
            CursorPage<NotificationResponse> result = sut.getNotifications(memberId, false, null, null, 1);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().id()).isEqualTo(100L);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isNotNull();
            assertThat(Cursor.decode(result.nextCursor(), IdCursor.class).id()).isEqualTo(100L);
        }

        @Test
        @DisplayName("success: returns empty result with null cursor when no notifications")
        void success_EmptyResult() {
            // given
            Long memberId = 1L;

            given(repository.findByKeyset(eq(memberId), eq(null), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            CursorPage<NotificationResponse> result = sut.getNotifications(memberId, false, null, null, 20);

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.hasNext()).isFalse();
        }
    }

    private Notification createMockNotification(Long id) {
        Notification notification = mock(Notification.class);
        given(notification.getId()).willReturn(id);
        given(notification.getEventId()).willReturn(UUID.randomUUID());
        given(notification.getNotificationType()).willReturn(NotificationType.ISSUE_ASSIGNED);
        given(notification.getMessage())
                .willReturn(new NotificationMessage(Map.of(NotificationDataKeys.ISSUE_KEY, "PROJ-1")));
        given(notification.getEntityReference()).willReturn(EntityReference.forIssue("PROJ", "PROJ-1"));
        given(notification.getActorMemberId()).willReturn(2L);
        given(notification.getActorDisplayName()).willReturn("actor");
        given(notification.isRead()).willReturn(false);
        given(notification.getCreatedAt()).willReturn(Instant.now());
        return notification;
    }
}
