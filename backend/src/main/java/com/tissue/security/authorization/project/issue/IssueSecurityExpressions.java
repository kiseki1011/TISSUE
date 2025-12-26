package com.tissue.security.authorization.project.issue;

import com.tissue.security.authentication.MemberUserDetails;

public interface IssueSecurityExpressions {

	/**
	 * @see IssueSecurityGuard#canEdit(String, String, String, MemberUserDetails)
	 */
	String REQUIRES_ISSUE_EDIT_PERMISSION = "@issueSecurityGuard.canEdit(#workspaceKey, #projectKey, #issueKey, principal)";

	/**
	 * @see IssueSecurityGuard#canDelete(String, String, String, MemberUserDetails)
	 */
	String REQUIRES_ISSUE_DELETE_PERMISSION = "@issueSecurityGuard.canDelete(#workspaceKey, #projectKey, #issueKey, principal)";

	/**
	 * @see IssueSecurityGuard#canManageParticipants(String, String, String, MemberUserDetails)
	 */
	String REQUIRES_ISSUE_PARTICIPANT_MANAGE_PERMISSION = "@issueSecurityGuard.canManageParticipants(#workspaceKey, #projectKey, #issueKey, principal)";

	/**
	 * @see IssueSecurityGuard#canManageReviewers(String, String, String, MemberUserDetails)
	 */
	String REQUIRES_ISSUE_REVIEWER_MANAGE_PERMISSION = "@issueSecurityGuard.canManageReviewers(#workspaceKey, #projectKey, #issueKey, principal)";
}
