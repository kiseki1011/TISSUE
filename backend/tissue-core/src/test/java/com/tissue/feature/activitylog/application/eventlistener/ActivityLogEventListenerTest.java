package com.tissue.feature.activitylog.application.eventlistener;

import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.ISSUE_KEY;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.TRIGGER_REASON;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.VCS_USER_EMAIL;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.VCS_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.tissue.feature.activitylog.application.dto.request.CreateLogWithDiffCommand;
import com.tissue.feature.activitylog.application.listener.IssueActivityLogListener;
import com.tissue.feature.activitylog.application.service.ActivityLogCommandService;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.feature.issue.domain.event.IssueTransitionedBySystemEvent;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityLogEventListenerTest {

    @Mock
    ActivityLogCommandService commandService;

    @InjectMocks
    IssueActivityLogListener sut;

    @Nested
    @DisplayName("handle transitioned by system")
    class HandleTransitionedBySystem {

        @Test
        @DisplayName("success: creates log with vcs info when all fields present")
        void successTransitionAllFieldsPresent() {
            // given
            IssueTransitionedBySystemEvent event = IssueTransitionedBySystemEvent.create(
                    "WORKSPACE",
                    "PROJ",
                    "PROJ-1",
                    null,
                    1L,
                    "Auto Merge",
                    100L,
                    "In Review",
                    101L,
                    "Done",
                    VcsProvider.GITHUB,
                    "user@github.com",
                    "github-user",
                    "GITHUB PR MERGED");

            // when
            sut.handleTransitionedBySystem(event);

            // then
            ArgumentCaptor<CreateLogWithDiffCommand> captor = ArgumentCaptor.forClass(CreateLogWithDiffCommand.class);
            then(commandService).should().createLogWithDiff(captor.capture());

            CreateLogWithDiffCommand cmd = captor.getValue();
            assertThat(cmd.activityType()).isEqualTo(ActivityType.ISSUE_WORKFLOW_TRANSITIONED_BY_SYSTEM);
            assertThat(cmd.actorMemberId()).isNull();
            assertThat(cmd.data())
                    .containsEntry(ISSUE_KEY, "PROJ-1")
                    .containsEntry(VCS_USER_NAME, "github-user")
                    .containsEntry(VCS_USER_EMAIL, "user@github.com")
                    .containsEntry(TRIGGER_REASON, "GITHUB PR MERGED");
        }

        @Test
        @DisplayName("success: falls back to UNKNOWN when vcs fields are null")
        void success_VcsNullFieldsFallbackToUnknown() {
            // given
            IssueTransitionedBySystemEvent event = IssueTransitionedBySystemEvent.create(
                    "WORKSPACE",
                    "PROJ",
                    "PROJ-1",
                    null,
                    1L,
                    "Auto Merge",
                    100L,
                    "In Review",
                    101L,
                    "Done",
                    VcsProvider.GITHUB,
                    null,
                    null,
                    "GITHUB PR MERGED");

            // when
            sut.handleTransitionedBySystem(event);

            // then
            ArgumentCaptor<CreateLogWithDiffCommand> captor = ArgumentCaptor.forClass(CreateLogWithDiffCommand.class);
            then(commandService).should().createLogWithDiff(captor.capture());

            CreateLogWithDiffCommand cmd = captor.getValue();
            assertThat(cmd.data()).containsEntry(VCS_USER_NAME, "UNKNOWN").containsEntry(VCS_USER_EMAIL, "UNKNOWN");
        }
    }
}
