package com.tissue.security.authorization;

import org.springframework.stereotype.Component;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.workspace.domain.enums.WorkspaceRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueSecurityGuard {

	private final ProjectSecurityGuard projectSecurityGuard;
	private final IssueQueryRepository issueQueryRepository;

	/**
	 * Checks whether the member has permission to modify the specified issue.
	 *
	 * <p>A member can modify an issue if they meet any of the following conditions:
	 * <ul>
	 *   <li>The member is a project administrator ({@link ProjectRole#ADMIN ProjectRole.ADMIN})</li>
	 *   <li>The member has workspace administrator privileges or higher
	 *       ({@link WorkspaceRole#ADMIN WorkspaceRole.ADMIN} or above)</li>
	 *   <li>The member is the author or assignee of the issue</li>
	 * </ul>
	 */
	public boolean canModify(String workspaceKey, String projectKey, String issueKey, Long memberId) {
		return checkIsAdminOrAuthorOrAssignee(workspaceKey, projectKey, issueKey, memberId);
	}

	/**
	 * Checks whether the member has permission to delete the specified issue.
	 *
	 * <p>A member can delete an issue if they meet any of the following conditions:
	 * <ul>
	 *   <li>The member is a project administrator ({@link ProjectRole#ADMIN ProjectRole.ADMIN})</li>
	 *   <li>The member has workspace administrator privileges or higher
	 *       ({@link WorkspaceRole#ADMIN WorkspaceRole.ADMIN} or above)</li>
	 *   <li>The member is the author of the issue</li>
	 * </ul>
	 */
	public boolean canDelete(String workspaceKey, String projectKey, String issueKey, Long memberId) {
		return checkIsAdminOrAuthor(workspaceKey, projectKey, issueKey, memberId);
	}

	/**
	 * Checks whether the member has permission to manage issue reviewers.
	 *
	 * <p>A member can manage reviewers of an issue if they meet any of the following conditions:
	 * <ul>
	 *   <li>The member is a project administrator ({@link ProjectRole#ADMIN ProjectRole.ADMIN})</li>
	 *   <li>The member has workspace administrator privileges or higher
	 *       ({@link WorkspaceRole#ADMIN WorkspaceRole.ADMIN} or above)</li>
	 *   <li>The member is the author or assignee of the issue</li>
	 * </ul>
	 */
	public boolean canManageReviewers(String workspaceKey, String projectKey, String issueKey, Long memberId) {
		return checkIsAdminOrAuthorOrAssignee(workspaceKey, projectKey, issueKey, memberId);
	}

	/**
	 * Checks whether the member has permission to manage issue participants.
	 *
	 * <p>A member can manage participants of an issue if they meet any of the following conditions:
	 * <ul>
	 *   <li>The member is a project administrator ({@link ProjectRole#ADMIN ProjectRole.ADMIN})</li>
	 *   <li>The member has workspace administrator privileges or higher
	 *       ({@link WorkspaceRole#ADMIN WorkspaceRole.ADMIN} or above)</li>
	 *   <li>The member is the author of the issue</li>
	 * </ul>
	 */
	public boolean canManageParticipants(String workspaceKey, String projectKey, String issueKey, Long memberId) {
		return checkIsAdminOrAuthor(workspaceKey, projectKey, issueKey, memberId);
	}

	private boolean checkIsAdminOrAuthorOrAssignee(String workspaceKey, String projectKey, String issueKey,
		Long memberId) {
		if (projectSecurityGuard.isAdmin(workspaceKey, projectKey, memberId)) {
			return true;
		}
		return issueQueryRepository.existsByKeysAndAuthorOrAssignee(workspaceKey, issueKey, memberId);
	}

	private boolean checkIsAdminOrAuthor(String workspaceKey, String projectKey, String issueKey, Long memberId) {
		if (projectSecurityGuard.isAdmin(workspaceKey, projectKey, memberId)) {
			return true;
		}
		return issueQueryRepository.existsByKeysAndAuthor(workspaceKey, issueKey, memberId);
	}
}
