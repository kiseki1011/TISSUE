package com.tissue.feature.workflow.application.listener;

import static com.tissue.feature.workflow.domain.guard.GuardType.APPROVAL_REQUIRED;
import static com.tissue.feature.workflow.domain.guard.types.ApprovalGuard.KEY_AUTO_REJECT;
import static com.tissue.feature.workflow.domain.guard.types.ApprovalGuard.KEY_REJECT_TRANSITION;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.port.usecase.IssueTransitionUseCase;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.feature.issue.domain.exception.IssueNotFoundException;
import com.tissue.feature.workflow.domain.TransitionGuardConfig;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.exception.AutoTransitionTargetNotFoundException;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// TODO: add javadoc
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowAutomationEventListener {

    private final IssueTransitionUseCase transitionUseCase;
    private final IssueQueryRepository issueQueryRepository;

    @EventListener
    public void handleReviewRejected(IssueReviewSubmittedEvent event) {
        if (event.reviewStatus() != ReviewStatus.CHANGES_REQUESTED) {
            return;
        }
        processAutoRejection(event);
    }

    private void processAutoRejection(IssueReviewSubmittedEvent event) {
        Issue issue = issueQueryRepository
                .findByKey(event.issueKey())
                .orElseThrow(() -> new IssueNotFoundException(event.issueKey()));

        List<WorkflowTransition> outgoingTransitions = getOutgoingTransitions(issue);

        String targetTransitionName =
                findAutoRejectTargetName(outgoingTransitions).orElse(null);

        if (targetTransitionName == null) {
            return;
        }

        WorkflowTransition targetTransition = findTransitionByName(outgoingTransitions, targetTransitionName)
                .orElseThrow(() -> new AutoTransitionTargetNotFoundException(
                        issue.getKey(), issue.getCurrentState().getDisplayName(), targetTransitionName));

        log.info("Auto-executing reject transition '{}' for issue {}", targetTransitionName, issue.getKey());

        IssueIdentifier iid = IssueIdentifier.ofIssueKey(issue.getKey());

        transitionUseCase.performTransition(iid, targetTransition.getId(), event.actorMemberId());
    }

    private List<WorkflowTransition> getOutgoingTransitions(Issue issue) {
        WorkflowState currentState = issue.getCurrentState();
        return currentState.getWorkflow().getTransitions().stream()
                .filter(t -> t.getSourceState().equals(currentState))
                .toList();
    }

    private Optional<String> findAutoRejectTargetName(List<WorkflowTransition> transitions) {
        return transitions.stream()
                .flatMap(t -> t.getGuardConfigs().stream())
                .filter(config -> config.getGuardType() == APPROVAL_REQUIRED)
                .map(TransitionGuardConfig::getGuardParams)
                .filter(this::isAutoRejectEnabled)
                .map(params -> (String) params.get(KEY_REJECT_TRANSITION))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private Optional<WorkflowTransition> findTransitionByName(List<WorkflowTransition> transitions, String name) {
        return transitions.stream()
                .filter(t -> t.getName().getDisplayName().equals(name))
                .findFirst();
    }

    private boolean isAutoRejectEnabled(Map<String, Object> params) {
        Object val = params.get(KEY_AUTO_REJECT);
        return (val instanceof Boolean b) ? b : false;
    }
}
