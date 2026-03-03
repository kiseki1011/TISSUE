package com.tissue.feature.issue.application.service.validator;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.CANNOT_DELETE_ISSUE_WITH_CHILDREN;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.exception.TransitionSourceStateMismatchException;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.shared.exception.base.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueValidator {

    private final IssueQueryRepository issueQueryRepo;

    public void ensureCanDelete(Issue issue) {
        ensureNoChildren(issue);
    }

    public void ensureValidTransition(Issue issue, String workspaceKey, WorkflowTransition transition) {
        boolean sourceStateMisMatch = !issue.getCurrentState().equals(transition.getSourceState());
        if (sourceStateMisMatch) {
            throw new TransitionSourceStateMismatchException(
                    workspaceKey,
                    issue.getKey(),
                    transition.getId(),
                    issue.getCurrentState().getDisplayName(),
                    transition.getSourceState().getDisplayName());
        }
    }

    private void ensureNoChildren(Issue issue) {
        boolean hasChildren = issueQueryRepo.hasChildren(issue.getWorkspaceKey(), issue.getKey());
        if (hasChildren) {
            throw new BadRequestException(CANNOT_DELETE_ISSUE_WITH_CHILDREN);
        }
    }
}
