package com.tissue.workflow.application.service.listener;

import static com.tissue.workflow.domain.guard.GuardType.REQUIRED_APPROVAL;
import static com.tissue.workflow.domain.guard.types.ApprovalGuard.KEY_AUTO_REJECT;
import static com.tissue.workflow.domain.guard.types.ApprovalGuard.KEY_REJECT_TRANSITION;

import com.tissue.issue.application.port.in.IssueTransitionUseCase;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.enums.ReviewStatus;
import com.tissue.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.issue.domain.exception.IssueNotFoundException;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.ProjectMember;
import com.tissue.workflow.domain.TransitionGuardConfig;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;
import com.tissue.workflow.domain.exception.AutoTransitionTargetNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowAutomationEventListener {

    private final IssueTransitionUseCase transitionUseCase;
    private final IssueQueryRepository issueQueryRepository;
    private final ProjectMemberFinder projectMemberFinder;

    @EventListener
    public void handleReviewRejected(IssueReviewSubmittedEvent event) {
        if (event.reviewStatus() != ReviewStatus.CHANGES_REQUESTED) {
            return;
        }
        processAutoRejection(event);
    }

    private void processAutoRejection(IssueReviewSubmittedEvent event) {
        Issue issue = issueQueryRepository
                .findByKeyAndWorkspaceKey(event.issueKey(), event.workspaceKey())
                .orElseThrow(() -> new IssueNotFoundException(event.workspaceKey(), event.issueKey()));

        List<WorkflowTransition> outgoingTransitions = getOutgoingTransitions(issue);

        String targetTransitionName =
                findAutoRejectTargetName(outgoingTransitions).orElse(null);

        if (targetTransitionName == null) {
            return;
        }

        WorkflowTransition targetTransition = findTransitionByName(outgoingTransitions, targetTransitionName)
                .orElseThrow(() -> new AutoTransitionTargetNotFoundException(
                        issue.getKey(), issue.getCurrentState().getDisplayName(), targetTransitionName));

        log.info(
                "Auto-executing reject transition '{}' for issue {} in workspace {}",
                targetTransitionName,
                issue.getKey(),
                issue.getWorkspaceKey());

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                event.workspaceKey(), event.projectKey(), event.actorMemberId());

        // TODO: Should i just directly depend on the service not the use-case?
        transitionUseCase.performTransition(issue.getKey(), targetTransition.getId(), ProjectMemberContext.from(actor));
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
                .filter(config -> config.getGuardType() == REQUIRED_APPROVAL)
                .map(TransitionGuardConfig::getGuardParams)
                .filter(this::isAutoRejectEnabled)
                .map(params -> (String) params.get(KEY_REJECT_TRANSITION))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private Optional<WorkflowTransition> findTransitionByName(List<WorkflowTransition> transitions, String name) {
        return transitions.stream()
                .filter(t -> t.getName().getDisplay().equals(name))
                .findFirst();
    }

    private boolean isAutoRejectEnabled(Map<String, Object> params) {
        Object val = params.get(KEY_AUTO_REJECT);
        return (val instanceof Boolean b) ? b : false;
    }
}
