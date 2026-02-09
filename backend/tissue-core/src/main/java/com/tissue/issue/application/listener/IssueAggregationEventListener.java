package com.tissue.issue.application.listener;

import com.tissue.issue.application.service.IssueAggregationService;
import com.tissue.issue.domain.event.IssueCreatedEvent;
import com.tissue.issue.domain.event.IssueDeletedEvent;
import com.tissue.issue.domain.event.IssueParentChangedEvent;
import com.tissue.issue.domain.event.IssueStoryPointChangedEvent;
import com.tissue.issue.domain.event.IssueTransitionedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class IssueAggregationEventListener {

    private final IssueAggregationService aggregationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStoryPointChange(IssueStoryPointChangedEvent event) {
        if (event.parentKey() != null) {
            log.debug("Syncing aggregation for parent issue {}.", event.parentKey());
            aggregationService.syncStatistics(event.workspaceKey(), event.issueKey());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleParentChange(IssueParentChangedEvent event) {
        if (event.oldParentKey() != null) {
            log.debug("Syncing aggregation for old parent {}.", event.oldParentKey());
            aggregationService.syncStatistics(event.workspaceKey(), event.oldParentKey());
        }
        if (event.newParentKey() != null) {
            log.debug("Syncing aggregation for new parent {}.", event.newParentKey());
            aggregationService.syncStatistics(event.workspaceKey(), event.newParentKey());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueCreated(IssueCreatedEvent event) {
        if (event.parentKey() != null) {
            log.debug("Syncing aggregation for parent {} due to child creation.", event.parentKey());
            aggregationService.syncStatistics(event.workspaceKey(), event.parentKey());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueDeleted(IssueDeletedEvent event) {
        if (event.parentKey() != null) {
            log.debug("Syncing aggregation for parent {} due to child deletion.", event.parentKey());
            aggregationService.syncStatistics(event.workspaceKey(), event.parentKey());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueTransitioned(IssueTransitionedEvent event) {
        if (event.parentKey() != null) {
            log.debug("Syncing aggregation for parent {} due to issue workflow transition.", event.parentKey());
            aggregationService.syncStatistics(event.workspaceKey(), event.parentKey());
        }
    }
}
