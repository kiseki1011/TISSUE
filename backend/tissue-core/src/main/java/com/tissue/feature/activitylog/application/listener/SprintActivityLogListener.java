package com.tissue.feature.activitylog.application.listener;

import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.ACTOR_DISPLAY_NAME;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.PROJECT_KEY;
import static com.tissue.feature.activitylog.domain.ActivityLogDataKeys.SPRINT_TITLE;

import com.tissue.feature.activitylog.application.dto.request.CreateLogCommand;
import com.tissue.feature.activitylog.application.service.ActivityLogCommandService;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.feature.sprint.domain.event.SprintCompletedEvent;
import com.tissue.feature.sprint.domain.event.SprintStartedEvent;
import com.tissue.shared.vo.EntityReference;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SprintActivityLogListener {

    private final ActivityLogCommandService activityLogCommandService;

    @EventListener
    public void handleSprintStarted(SprintStartedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.SPRINT_STARTED,
                EntityReference.forSprint(event.projectKey(), event.sprintId()),
                event.actorMemberId(),
                Map.of(
                        PROJECT_KEY,
                        event.projectKey(),
                        SPRINT_TITLE,
                        event.sprintTitle(),
                        ACTOR_DISPLAY_NAME,
                        event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }

    @EventListener
    public void handleSprintCompleted(SprintCompletedEvent event) {
        CreateLogCommand cmd = new CreateLogCommand(
                event.eventId(),
                ActivityType.SPRINT_COMPLETED,
                EntityReference.forSprint(event.projectKey(), event.sprintId()),
                event.actorMemberId(),
                Map.of(
                        PROJECT_KEY,
                        event.projectKey(),
                        SPRINT_TITLE,
                        event.sprintTitle(),
                        ACTOR_DISPLAY_NAME,
                        event.actorDisplayName()));

        activityLogCommandService.createLog(cmd);
    }
}
