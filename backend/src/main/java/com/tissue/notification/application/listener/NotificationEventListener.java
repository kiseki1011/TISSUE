package com.tissue.notification.application.listener;

import com.tissue.issue.domain.event.IssueCreatedEvent;
import com.tissue.notification.application.service.NotificationCommandService;
import com.tissue.notification.application.service.command.NotificationTargetService;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.vo.EntityReference;
import com.tissue.workspace.application.port.out.WorkspaceMemberContact;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationCommandService commandService;
    private final NotificationTargetService targetService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueCreated(IssueCreatedEvent event) {
        log.info("Handling IssueCreatedEvent for issue: {}", event.issueKey());

        List<WorkspaceMemberContact> targets =
                targetService.getAllWorkspaceMembersExcluding(event.workspaceKey(), event.actorMemberId());

        EntityReference reference =
                EntityReference.forIssue(event.workspaceKey(), event.projectKey(), event.issueKey(), event.issueId());

        commandService.createAndSend(
                event.eventId(),
                NotificationType.ISSUE_CREATED,
                reference,
                targets,
                event.actorMemberId(),
                event.actorDisplayName(),
                // Args: {0}=ProjectKey, {1}=IssueKey, {2}=ActorName
                event.projectKey(),
                event.issueKey(),
                event.actorDisplayName());
    }
}
