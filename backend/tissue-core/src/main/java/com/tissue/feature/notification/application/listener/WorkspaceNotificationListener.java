package com.tissue.feature.notification.application.listener;

import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ACTOR_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.JOINED_MEMBER_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.NEW_ROLE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.OLD_ROLE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ROLE;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.TARGET_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.WORKSPACE_KEY;

import com.tissue.feature.notification.application.service.NotificationCommandService;
import com.tissue.feature.notification.application.service.NotificationTargetService;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberContactInfo;
import com.tissue.feature.workspace.domain.event.MemberJoinedWorkspaceEvent;
import com.tissue.feature.workspace.domain.event.WorkspaceOwnershipTransferredEvent;
import com.tissue.feature.workspace.domain.event.WorkspaceRoleChangedEvent;
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
public class WorkspaceNotificationListener {

    private final NotificationCommandService commandService;
    private final NotificationTargetService targetService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberJoinedWorkspace(MemberJoinedWorkspaceEvent event) {
        Collection<WorkspaceMemberContactInfo> targets = targetService.getWorkspaceAdmins(event.workspaceKey());

        targets.removeIf(t ->
                t.getMemberId().equals(event.actorMemberId()) || t.getMemberId().equals(event.joinedMemberId()));

        log.info(
                "Handling MemberJoinedWorkspaceEvent: member={}, workspace={}, targets={}",
                event.joinedMemberId(),
                event.workspaceKey(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forWorkspaceMember(event.workspaceKey(), event.joinedMemberId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.MEMBER_JOINED_WORKSPACE,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY, event.workspaceKey(),
                        JOINED_MEMBER_NAME, event.joinedMemberDisplayName(),
                        ROLE, event.role().name()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWorkspaceRoleChanged(WorkspaceRoleChangedEvent event) {
        if (event.targetMemberId().equals(event.actorMemberId())) {
            return;
        }

        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getSpecificMemberTarget(event.workspaceKey(), event.targetMemberId());

        log.info(
                "Handling WorkspaceRoleChangedEvent: targetMember={}, newRole={}, targets={}",
                event.targetMemberId(),
                event.newRole(),
                targets.size());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forWorkspaceMember(event.workspaceKey(), event.targetMemberId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.WORKSPACE_ROLE_CHANGED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                Map.of(
                        WORKSPACE_KEY,
                        event.workspaceKey(),
                        TARGET_NAME,
                        event.targetDisplayName(),
                        ACTOR_NAME,
                        event.actorDisplayName(),
                        OLD_ROLE,
                        event.oldRole().name(),
                        NEW_ROLE,
                        event.newRole().name()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOwnershipTransferred(WorkspaceOwnershipTransferredEvent event) {
        Collection<WorkspaceMemberContactInfo> targets =
                targetService.getSpecificMemberTarget(event.workspaceKey(), event.newOwnerMemberId());

        log.info(
                "Handling WorkspaceOwnershipTransferredEvent: newOwner={}, previousOwner={}, workspace={}",
                event.newOwnerMemberId(),
                event.previousOwnerMemberId(),
                event.workspaceKey());

        if (targets.isEmpty()) {
            return;
        }

        EntityReference reference = EntityReference.forWorkspaceMember(event.workspaceKey(), event.newOwnerMemberId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.WORKSPACE_OWNERSHIP_TRANSFERRED,
                reference,
                targets,
                event.previousOwnerMemberId(),
                event.previousOwnerDisplayName(),
                Map.of(WORKSPACE_KEY, event.workspaceKey(), ACTOR_NAME, event.previousOwnerDisplayName()));
    }
}
