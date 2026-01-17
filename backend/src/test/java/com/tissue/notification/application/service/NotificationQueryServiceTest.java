package com.tissue.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.tissue.common.dto.CursorPageResponse;
import com.tissue.common.enums.SupportedLanguage;
import com.tissue.common.vo.EntityReference;
import com.tissue.notification.application.dto.response.NotificationResponse;
import com.tissue.notification.application.port.out.NotificationRepository;
import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.vo.NotificationMessage;
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
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock
    NotificationRepository repository;

    @Mock
    MessageSource messageSource;

    @InjectMocks
    NotificationQueryService sut;

    @Nested
    @DisplayName("get notifications")
    class GetNotifications {
        @Test
        @DisplayName("success: returns mapped responses")
        void success_GetNotifications() {
            String workspaceKey = "TESTWS";
            Long memberId = 1L;
            boolean unreadOnly = false;
            Long cursorId = null;
            int limit = 20;

            Notification notification = Notification.builder()
                    .eventId(UUID.randomUUID())
                    .notificationType(NotificationType.ISSUE_CREATED)
                    .entityReference(EntityReference.forIssue(workspaceKey, "TESTPROJ", "TESTPROJ-1", 100L))
                    .actorMemberId(2L)
                    .actorDisplayName("Actor")
                    .receiverMemberId(memberId)
                    .receiverEmail("test@test.com")
                    .receiverLanguage(SupportedLanguage.EN)
                    .message(new NotificationMessage(Map.of("issueKey", "TESTPROJ-1")))
                    .build();

            ReflectionTestUtils.setField(notification, "id", 100L);

            given(repository.findByCursor(
                            ArgumentMatchers.eq(memberId),
                            ArgumentMatchers.eq(workspaceKey),
                            ArgumentMatchers.eq(cursorId),
                            ArgumentMatchers.any(Pageable.class)))
                    .willReturn(List.of(notification));

            // mock message source
            given(messageSource.getMessage(
                            "event.ISSUE_CREATED.title",
                            null,
                            "event.ISSUE_CREATED.title",
                            LocaleContextHolder.getLocale()))
                    .willReturn("Issue Created: {issueKey}");
            given(messageSource.getMessage(
                            "event.ISSUE_CREATED.content",
                            null,
                            "event.ISSUE_CREATED.content",
                            LocaleContextHolder.getLocale()))
                    .willReturn("Check it out");

            CursorPageResponse<NotificationResponse> result =
                    sut.getNotifications(workspaceKey, memberId, unreadOnly, cursorId, limit);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).title()).isEqualTo("Issue Created: TESTPROJ-1");
            assertThat(result.nextCursorId()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("check unread status")
    class CheckUnreadStatus {
        @Test
        @DisplayName("success: returns true if unread notification exists")
        void success_CheckUnreadStatus() {
            String workspaceKey = "TESTWS";
            Long memberId = 1L;
            given(repository.existsByReceiverMemberIdAndEntityReference_WorkspaceKeyAndIsReadFalse(
                            memberId, workspaceKey))
                    .willReturn(true);

            boolean result = sut.checkUnreadStatus(workspaceKey, memberId);

            assertThat(result).isTrue();
        }
    }
}
