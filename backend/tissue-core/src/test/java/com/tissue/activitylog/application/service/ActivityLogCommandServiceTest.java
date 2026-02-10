package com.tissue.activitylog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.tissue.feature.activitylog.application.dto.request.CreateLogCommand;
import com.tissue.feature.activitylog.application.dto.request.CreateLogWithDiffCommand;
import com.tissue.feature.activitylog.application.port.out.ActivityLogRepository;
import com.tissue.feature.activitylog.application.service.ActivityLogCommandService;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.shared.dto.FieldChange;
import com.tissue.shared.vo.EntityReference;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityLogCommandServiceTest {

    @Mock
    ActivityLogRepository repository;

    @InjectMocks
    ActivityLogCommandService sut;

    @Nested
    @DisplayName("create log")
    class CreateLog {

        @Test
        @DisplayName("success: saves activity log without changes")
        void success_CreateLog() {
            CreateLogCommand cmd = new CreateLogCommand(
                    UUID.randomUUID(),
                    ActivityType.ISSUE_CREATED,
                    EntityReference.forIssue("TESTWS", "TESTPROJ", "TESTPROJ-1"),
                    100L,
                    Map.of("key", "value"));

            sut.createLog(cmd);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            then(repository).should().save(captor.capture());

            ActivityLog saved = captor.getValue();
            assertThat(saved.getEventId()).isEqualTo(cmd.eventId());
            assertThat(saved.getActivityType()).isEqualTo(cmd.activityType());
            assertThat(saved.getEntityReference()).isEqualTo(cmd.reference());
            assertThat(saved.getActorMemberId()).isEqualTo(cmd.actorMemberId());
            assertThat(saved.getData()).isEqualTo(cmd.data());
            assertThat(saved.getChanges()).isNull();
        }
    }

    @Nested
    @DisplayName("create log with diff")
    class CreateLogWithDiff {

        @Test
        @DisplayName("success: saves activity log with changes")
        void success_CreateLogWithDiff() {
            CreateLogWithDiffCommand cmd = new CreateLogWithDiffCommand(
                    UUID.randomUUID(),
                    ActivityType.ISSUE_UPDATED,
                    EntityReference.forIssue("TESTWS", "TESTPROJ", "TESTPROJ-1"),
                    100L,
                    Map.of("key", "value"),
                    Map.of("field", new FieldChange("old", "new")));

            sut.createLogWithDiff(cmd);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            then(repository).should().save(captor.capture());

            ActivityLog saved = captor.getValue();
            assertThat(saved.getEventId()).isEqualTo(cmd.eventId());
            assertThat(saved.getActivityType()).isEqualTo(cmd.activityType());
            assertThat(saved.getEntityReference()).isEqualTo(cmd.reference());
            assertThat(saved.getActorMemberId()).isEqualTo(cmd.actorMemberId());
            assertThat(saved.getData()).isEqualTo(cmd.data());
            assertThat(saved.getChanges()).isEqualTo(cmd.changes());
        }
    }
}
