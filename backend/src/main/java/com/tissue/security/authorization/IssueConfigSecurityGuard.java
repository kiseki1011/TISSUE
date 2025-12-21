package com.tissue.security.authorization;

import org.springframework.stereotype.Component;

import com.tissue.issuetype.application.port.out.IssueTypeQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueConfigSecurityGuard {

	private final ProjectSecurityGuard projectSecurityGuard;
	private final IssueTypeQueryRepository issueTypeRepository;

	public boolean canManageIssueType(
		String workspaceKey,
		String projectKey,
		Long issueTypeId,
		Long memberId
	) {
		if (projectSecurityGuard.isAdmin(workspaceKey, projectKey, memberId)) {
			return true;
		}

		return issueTypeRepository.findByIdAndProjectKey(issueTypeId, projectKey)
			.map(issueType -> issueType.getCreatedBy().equals(memberId))
			.orElse(false);
	}
}
