package com.tissue.feature.notification.application.listener;

import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ENDED_AT;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.PROJECT_KEY;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.SPRINT_TITLE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.STARTED_AT;

import com.tissue.feature.member.application.port.repository.MemberContactInfo;
import com.tissue.feature.notification.application.service.NotificationCommandService;
import com.tissue.feature.notification.application.service.NotificationTargetService;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.sprint.domain.event.SprintCompletedEvent;
import com.tissue.feature.sprint.domain.event.SprintStartedEvent;
import com.tissue.shared.vo.EntityReference;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SprintNotificationListener {

    private final NotificationCommandService commandService;
    private final NotificationTargetService targetService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSprintStarted(SprintStartedEvent event) {
        Collection<MemberContactInfo> targets = targetService.getAllProjectMembers(event.projectKey());

        removeActorFromTargets(targets, event.actorMemberId());

        log.info("Handling SprintStartedEvent: sprint={}, targets={}", event.sprintTitle(), targets.size());

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

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSprintCompleted(SprintCompletedEvent event) {
        Collection<MemberContactInfo> targets = targetService.getAllProjectMembers(event.projectKey());

        removeActorFromTargets(targets, event.actorMemberId());

        log.info("Handling SprintCompletedEvent: sprint={}, targets={}", event.sprintTitle(), targets.size());

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
