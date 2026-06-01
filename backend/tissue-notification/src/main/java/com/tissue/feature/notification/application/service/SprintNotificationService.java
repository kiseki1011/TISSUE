package com.tissue.feature.notification.application.service;

import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ENDED_AT;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.PROJECT_KEY;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.SPRINT_TITLE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.STARTED_AT;

import com.tissue.feature.member.application.port.repository.MemberContactInfo;
import com.tissue.feature.notification.application.port.usecase.SprintNotificationUseCase;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.sprint.domain.event.SprintCompletedEvent;
import com.tissue.feature.sprint.domain.event.SprintStartedEvent;
import com.tissue.shared.vo.EntityReference;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SprintNotificationService implements SprintNotificationUseCase {

    private final NotificationCommandService commandService;
    private final NotificationTargetService targetService;

    @Override
    public void handleSprintStarted(SprintStartedEvent event) {
        Collection<MemberContactInfo> targets = targetService.getAllProjectMembers(event.projectKey());

        removeActorFromTargets(targets, event.actorMemberId());

        log.debug("Handling SprintStartedEvent: sprint={}, targets={}", event.sprintTitle(), targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forSprint(event.projectKey(), event.sprintId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.SPRINT_STARTED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        PROJECT_KEY, event.projectKey(),
                        SPRINT_TITLE, event.sprintTitle()));
    }

    @Override
    public void handleSprintCompleted(SprintCompletedEvent event) {
        Collection<MemberContactInfo> targets = targetService.getAllProjectMembers(event.projectKey());

        removeActorFromTargets(targets, event.actorMemberId());

        log.debug("Handling SprintCompletedEvent: sprint={}, targets={}", event.sprintTitle(), targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forSprint(event.projectKey(), event.sprintId());

        String startedAt = event.startedAt() != null ? event.startedAt().toString() : "";
        String endedAt = event.endedAt() != null ? event.endedAt().toString() : "";

        commandService.createAndSend(
                event.eventId(),
                NotificationType.SPRINT_COMPLETED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        PROJECT_KEY,
                        event.projectKey(),
                        SPRINT_TITLE,
                        event.sprintTitle(),
                        STARTED_AT,
                        startedAt,
                        ENDED_AT,
                        endedAt));
    }

    private void removeActorFromTargets(Collection<MemberContactInfo> targets, Long memberId) {
        if (targets == null || memberId == null) {
            return;
        }
        targets.removeIf(target -> target.getMemberId().equals(memberId));
    }
}
