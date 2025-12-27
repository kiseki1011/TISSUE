package com.tissue.issue.application.service.authorization;

import com.tissue.security.authentication.MemberUserDetails;

public interface IssueAuthExpressions {

	/**
	 * @see IssueAuthorizationService#canEdit(String, String, String, MemberUserDetails)
	 */
	String REQUIRES_ISSUE_EDIT_PERMISSION = "@issueSecurityGuard.canEdit(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueKey, principal)";

	/**
	 * @see IssueAuthorizationService#canDelete(String, String, String, MemberUserDetails)
	 */
	String REQUIRES_ISSUE_DELETE_PERMISSION = "@issueSecurityGuard.canDelete(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueKey, principal)";

	/**
	 * @see IssueAuthorizationService#canManageParticipants(String, String, String, MemberUserDetails)
	 */
	String REQUIRES_ISSUE_PARTICIPANT_MANAGE_PERMISSION = "@issueSecurityGuard.canManageParticipants(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueKey, principal)";

	/**
	 * @see IssueAuthorizationService#canManageReviewers(String, String, String, MemberUserDetails)
	 */
	String REQUIRES_ISSUE_REVIEWER_MANAGE_PERMISSION = "@issueSecurityGuard.canManageReviewers(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueKey, principal)";
}
