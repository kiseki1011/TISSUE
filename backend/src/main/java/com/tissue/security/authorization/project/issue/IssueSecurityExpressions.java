package com.tissue.security.authorization.project.issue;

public interface IssueSecurityExpressions {

	String REQUIRES_ISSUE_EDITOR = "@issueSecurityGuard.canModify(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueKey, principal.memberId)";

	String REQUIRES_ISSUE_DELETER = "@issueSecurityGuard.isAuthor(#cmd.issueKey, principal.memberId)";

	String REQUIRES_ISSUE_PARTICIPANT_MANAGER = "@issueSecurityGuard.canManageParticipants(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueKey, principal.memberId)";

	String REQUIRES_ISSUE_REVIEWER_MANAGER = "@issueSecurityGuard.canManageReviewers(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueKey, principal.memberId)";
}
