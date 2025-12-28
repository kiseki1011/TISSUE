package com.tissue.issue.application.service.validator;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.exception.IssueExceptions;
import com.tissue.workflow.domain.WorkflowTransition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueValidator {

    private final IssueQueryRepository issueQueryRepo;

    public void ensureCanDelete(Issue issue) {
        ensureNoChildren(issue);
    }

    public void ensureValidTransition(
            Issue issue, Long transitionId, String workspaceKey, WorkflowTransition transition) {
        boolean sourceStateMisMatch = !issue.getCurrentState().equals(transition.getSourceState());
        if (sourceStateMisMatch) {
            throw IssueExceptions.transitionSourceStateMismatch(
                    workspaceKey,
                    issue.getKey(),
                    transitionId,
                    issue.getCurrentState().getDisplayName(),
                    transition.getSourceState().getDisplayName());
        }
    }

    private void ensureNoChildren(Issue issue) {
        boolean hasChildren = issueQueryRepo.hasChildren(issue.getWorkspaceKey(), issue.getKey());
        if (hasChildren) {
            throw IssueExceptions.cannotDeleteIssueWithChildren(issue.getKey());
        }
    }
}
