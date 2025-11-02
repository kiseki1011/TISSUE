package com.tissue.api.issue.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.Issue;

import lombok.RequiredArgsConstructor;

// TODO: 이건 애플리케이션 계층에 속하나? 아니면 도메인 서비스에 속하나?
@Component
@RequiredArgsConstructor
public class IssueValidator {
	private final IssueQueryRepository issueQueryRepo;

	public void ensureCanDelete(Issue issue) {
		ensureNoChildren(issue);
	}

	private void ensureNoChildren(Issue issue) {
		boolean hasChildren = issueQueryRepo.hasChildren(issue.getWorkspaceKey(), issue.getKey());
		if (hasChildren) {
			throw new InvalidOperationException(
				"Cannot delete issue that has children. issueKey: %s"
					.formatted(issue.getKey())
			);
		}
	}
}
