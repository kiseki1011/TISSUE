package com.tissue.issue.application.service.validator;

import static com.tissue.issue.domain.exception.IssueErrorCode.*;

import org.springframework.stereotype.Component;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.Issue;
import com.tissue.workflow.domain.WorkflowTransition;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueValidator {

	private final IssueQueryRepository issueQueryRepo;

	public void ensureCanDelete(Issue issue) {
		ensureNoChildren(issue);
	}

	public void ensureValidTransition(
		Issue issue,
		Long transitionId,
		String workspaceKey,
		WorkflowTransition transition
	) {
		boolean sourceStateMisMatch = !issue.getCurrentState().equals(transition.getSourceState());
		if (sourceStateMisMatch) {
			throw new BadRequestException(TRANSITION_SOURCE_STATE_NOT_MATCH)
				.addContext("workspaceKey", workspaceKey)
				.addContext("issueKey", issue.getKey())
				.addContext("transitionId", transitionId)
				.addContext("currentState", issue.getCurrentState().getDisplayLabel())
				.addContext("requiredState", transition.getSourceState().getDisplayLabel());
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
