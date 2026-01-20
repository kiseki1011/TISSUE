package com.tissue.notification.application.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.tissue.common.enums.SupportedLanguage;
import com.tissue.issue.domain.event.IssueCreatedEvent;
import com.tissue.notification.application.service.NotificationCommandService;
import com.tissue.notification.application.service.NotificationTargetService;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.workspace.application.port.out.WorkspaceMemberContact;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    NotificationCommandService commandService;

    @Mock
    NotificationTargetService targetService;

    @InjectMocks
    NotificationEventListener sut;

    @Nested
    @DisplayName("handle issue created")
    class HandleIssueCreated {
        @Test
        @DisplayName("success: sends notification to project members")
        void success_HandleIssueCreated() {
            // spotless:off
            IssueCreatedEvent event =
                    IssueCreatedEvent.create(
                        "TESTWS",
                        "TESTPROJ",
                        1L,
                        "TESTPROJ-1",
                        100L,
                        null,
                        null,
                        1L,
                        "Actor");
            // spotless:on

            WorkspaceMemberContact contact = new WorkspaceMemberContact(2L, "user2@test.com", SupportedLanguage.EN);
            given(targetService.getProjectMembersExcluding("TESTWS", "TESTPROJ", 1L))
                    .willReturn(List.of(contact));

            sut.handleIssueCreated(event);

            then(commandService)
                    .should()
                    .createAndSend(
                            eq(event.eventId()),
                            eq(NotificationType.ISSUE_CREATED),
                            any(),
                            eq(List.of(contact)),
                            eq(1L),
                            eq("Actor"),
                            anyMap());
        }

        @Test
        @DisplayName("success: calls createAndSend even if no targets")
        void success_HandleIssueCreated_NoTargets() {
            // spotless:off
            IssueCreatedEvent event =
                    IssueCreatedEvent.create(
                        "TESTWS",
                        "TESTPROJ",
                        1L,
                        "TESTPROJ-1",
                        100L,
                        null,
                        null,
                        1L,
                        "Actor");
            // spotless:on

            given(targetService.getProjectMembersExcluding("TESTWS", "TESTPROJ", 1L))
                    .willReturn(Collections.emptyList());

            sut.handleIssueCreated(event);

            then(commandService)
                    .should()
                    .createAndSend(
                            eq(event.eventId()),
                            eq(NotificationType.ISSUE_CREATED),
                            any(),
                            eq(Collections.emptyList()),
                            eq(1L),
                            eq("Actor"),
                            anyMap());
        }
    }
}
