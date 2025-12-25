package com.tissue.security.authorization.project.issue;

import org.springframework.stereotype.Component;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authorization.project.ProjectSecurityGuard;
import com.tissue.workspace.domain.enums.WorkspaceRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueSecurityGuard {

	private final IssueQueryRepository issueQueryRepository;
	private final ProjectSecurityGuard projectSecurityGuard;

	/**
	 * Checks whether the member has permission to modify the specified issue.
	 *
	 * <p>A member can modify an issue if they meet any of the following conditions:
	 * <ul>
	 *   <li>The member is a project administrator ({@link ProjectRole#ADMIN})</li>
	 *   <li>The member has workspace administrator privileges or higher
	 *       ({@link WorkspaceRole#ADMIN} or above)</li>
	 *   <li>The member is the author or assignee of the issue</li>
	 * </ul>
	 */
	public boolean canEdit(String workspaceKey, String projectKey, String issueKey, MemberUserDetails userDetails) {
		if (projectSecurityGuard.isAdmin(workspaceKey, projectKey, userDetails)) {
			return true;
		}
		return issueQueryRepository.isAuthorOrAssignee(workspaceKey, issueKey, userDetails.getMemberId());
	}

	/**
	 * Checks whether the member has permission to delete the specified issue.
	 *
	 * <p>A member can delete an issue if they meet any of the following conditions:
	 * <ul>
	 *   <li>The member is a project administrator ({@link ProjectRole#ADMIN})</li>
	 *   <li>The member has workspace administrator privileges or higher
	 *       ({@link WorkspaceRole#ADMIN} or above)</li>
	 *   <li>The member is the author of the issue</li>
	 * </ul>
	 */
	public boolean canDelete(String workspaceKey, String projectKey, String issueKey, MemberUserDetails userDetails) {
		if (projectSecurityGuard.isAdmin(workspaceKey, projectKey, userDetails)) {
			return true;
		}
		return issueQueryRepository.isAuthor(workspaceKey, issueKey, userDetails.getMemberId());
	}

	/**
	 * Checks whether the member has permission to manage issue reviewers.
	 *
	 * <p>A member can manage reviewers of an issue if they meet any of the following conditions:
	 * <ul>
	 *   <li>The member is a project administrator ({@link ProjectRole#ADMIN})</li>
	 *   <li>The member has workspace administrator privileges or higher
	 *       ({@link WorkspaceRole#ADMIN} or above)</li>
	 *   <li>The member is the author or assignee of the issue</li>
	 * </ul>
	 */
	public boolean canManageReviewers(String workspaceKey, String projectKey, String issueKey,
		MemberUserDetails userDetails) {
		return canEdit(workspaceKey, projectKey, issueKey, userDetails);
	}

	/**
	 * Checks whether the member has permission to manage issue participants.
	 *
	 * <p>A member can manage participants of an issue if they meet any of the following conditions:
	 * <ul>
	 *   <li>The member is a project administrator ({@link ProjectRole#ADMIN})</li>
	 *   <li>The member has workspace administrator privileges or higher
	 *       ({@link WorkspaceRole#ADMIN} or above)</li>
	 *   <li>The member is the author of the issue</li>
	 * </ul>
	 */
	public boolean canManageParticipants(String workspaceKey, String projectKey, String issueKey,
		MemberUserDetails userDetails) {
		return canDelete(workspaceKey, projectKey, issueKey, userDetails);
	}
}
