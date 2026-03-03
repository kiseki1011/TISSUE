package com.tissue.activitylog.application.eventlistener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

import com.tissue.feature.activitylog.application.dto.request.CreateLogCommand;
import com.tissue.feature.activitylog.application.dto.request.CreateLogWithDiffCommand;
import com.tissue.feature.activitylog.application.listener.ActivityLogEventListener;
import com.tissue.feature.activitylog.application.service.ActivityLogCommandService;
import com.tissue.feature.issue.domain.event.IssueCreatedEvent;
import com.tissue.feature.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedEvent;
import com.tissue.shared.dto.FieldChange;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityLogEventListenerTest {

    @Mock
    ActivityLogCommandService commandService;

    @InjectMocks
    ActivityLogEventListener sut;

    @Nested
    @DisplayName("handle issue created")
    class HandleIssueCreated {
        @Test
        @DisplayName("success: creates activity log")
        void success_HandleIssueCreated() {
            // spotless:off
            IssueCreatedEvent event =
                    IssueCreatedEvent.create(
                        "TESTWS",
                        "TESTPROJ",
                        "TESTPROJ-1",
                        null,
                        1L,
                        "TestUser");
            // spotless:on

            sut.handleIssueCreated(event);

            then(commandService).should().createLog(any(CreateLogCommand.class));
        }
    }

    @Nested
    @DisplayName("handle issue updated")
    class HandleIssueUpdated {
        @Test
        @DisplayName("success: creates activity log with diff")
        void success_HandleIssueUpdated() {
            IssueFieldsUpdatedEvent event = IssueFieldsUpdatedEvent.create(
                    "TESTWS",
                    "TESTPROJ",
                    "TESTPROJ-1",
                    Map.of("title", new FieldChange("Old Title", "New Title")),
                    1L,
                    "TestUser");

            sut.handleIssueUpdated(event);

            then(commandService).should().createLogWithDiff(any(CreateLogWithDiffCommand.class));
        }
    }

    @Nested
    @DisplayName("handle issue transitioned")
    class HandleIssueTransitioned {
        @Test
        @DisplayName("success: creates activity log with status diff")
        void success_HandleIssueTransitioned() {
            IssueTransitionedEvent event = IssueTransitionedEvent.create(
                    "TESTWS",
                    "TESTPROJ",
                    "TESTPROJ-1",
                    null,
                    123L,
                    "Transition",
                    100L,
                    "Todo",
                    101L,
                    "In Progress",
                    1L,
                    "TestUser");

            sut.handleIssueTransitioned(event);

            then(commandService).should().createLogWithDiff(any(CreateLogWithDiffCommand.class));
        }
    }
}
