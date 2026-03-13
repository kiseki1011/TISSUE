package com.tissue.feature.notification.application.eventlistener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.issue.domain.event.IssueCreatedEvent;
import com.tissue.feature.notification.application.listener.NotificationEventListener;
import com.tissue.feature.notification.application.service.NotificationCommandService;
import com.tissue.feature.notification.application.service.NotificationTargetService;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberContactInfo;
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
                        "TESTPROJ-1",
                        null,
                        1L,
                        "Actor");
            // spotless:on

            WorkspaceMemberContactInfo contact = mock(WorkspaceMemberContactInfo.class);
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
                        "TESTPROJ-1",
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
