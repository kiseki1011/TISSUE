package com.tissue.feature.notification.application.service;

import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ACTOR_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.NEW_ROLE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.OLD_ROLE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.PROJECT_KEY;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.TARGET_NAME;

import com.tissue.feature.member.application.port.repository.MemberContactInfo;
import com.tissue.feature.notification.application.port.usecase.ProjectNotificationUseCase;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.project.domain.event.ProjectRoleChangedEvent;
import com.tissue.shared.vo.EntityReference;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectNotificationService implements ProjectNotificationUseCase {

    private final NotificationCommandService commandService;
    private final NotificationTargetService targetService;

    @Override
    public void handleProjectRoleChanged(ProjectRoleChangedEvent event) {
        Collection<MemberContactInfo> targets = targetService.getSpecificMemberTarget(event.targetMemberId());

        log.debug(
                "Handling ProjectRoleChangedEvent: project={}, target={}, {} -> {}",
                event.projectKey(),
                event.targetMemberId(),
                event.oldRole(),
                event.newRole());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forProjectMember(event.projectKey(), event.targetMemberId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.PROJECT_ROLE_CHANGED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        PROJECT_KEY,
                        event.projectKey(),
                        ACTOR_NAME,
                        event.actorDisplayName(),
                        TARGET_NAME,
                        event.targetDisplayName(),
                        OLD_ROLE,
                        event.oldRole().name(),
                        NEW_ROLE,
                        event.newRole().name()));
    }
}
