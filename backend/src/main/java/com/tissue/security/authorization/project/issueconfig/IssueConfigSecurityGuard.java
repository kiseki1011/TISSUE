package com.tissue.security.authorization.project.issueconfig;

import org.springframework.stereotype.Component;

import com.tissue.issuetype.application.port.out.IssueTypeQueryRepository;
import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authorization.project.ProjectSecurityGuard;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueConfigSecurityGuard {

	private final ProjectSecurityGuard projectSecurityGuard;
	private final IssueTypeQueryRepository issueTypeRepository;

	public boolean canEditIssueType(String workspaceKey, String projectKey, Long issueTypeId,
		MemberUserDetails userDetails) {
		return projectSecurityGuard.isAdmin(workspaceKey, projectKey, userDetails)
			|| isIssueTypeCreator(projectKey, issueTypeId, userDetails);
	}

	private Boolean isIssueTypeCreator(String projectKey, Long issueTypeId, MemberUserDetails userDetails) {
		return issueTypeRepository.findByIdAndProjectKey(issueTypeId, projectKey)
			.map(issueType -> issueType.getCreatedBy().equals(userDetails.getMemberId()))
			.orElse(false);
	}
}
