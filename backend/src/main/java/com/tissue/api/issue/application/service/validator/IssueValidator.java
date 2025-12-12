package com.tissue.api.issue.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.exception.InvalidStateTransitionException;
import com.tissue.api.workflow.domain.WorkflowTransition;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueValidator {

	private final IssueQueryRepository issueQueryRepo;

	public void ensureCanDelete(Issue issue) {
		ensureNoChildren(issue);
	}

	public void ensureValidTransition(Issue issue, Long transitionId, String workspaceKey,
		WorkflowTransition transition) {
		boolean transitionSourceStateNotMatch = !issue.getCurrentState().equals(transition.getSourceState());
		if (transitionSourceStateNotMatch) {
			throw new InvalidStateTransitionException(
				issue.getKey(), "projectKey", workspaceKey, transitionId,
				issue.getCurrentState().getDisplayLabel(), transition.getSourceState().getDisplayLabel()
			);
		}
	}

	private void ensureNoChildren(Issue issue) {
		boolean hasChildren = issueQueryRepo.hasChildren(issue.getWorkspaceKey(), issue.getKey());
		if (hasChildren) {
			// TODO: IssueChildrenExistsException? 더 좋은 이름이 있을까?
			throw new RuntimeException(
				"Cannot delete issue that has children. issueKey: %s"
					.formatted(issue.getKey())
			);
		}
	}
}
