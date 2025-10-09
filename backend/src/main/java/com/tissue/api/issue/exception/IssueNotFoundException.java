package com.tissue.api.issue.exception;

import com.tissue.api.common.exception.type.ResourceNotFoundException;

public class IssueNotFoundException extends ResourceNotFoundException {

	private static final String ISSUE_KEY_MESSAGE = "Issue(%s) not found.";
	private static final String ISSUE_KEY_CODE_MESSAGE = "Issue(%s) not found in workspace(%s).";
	private static final String ISSUE_KEY_SPRINT_KEY_CODE_MESSAGE = "Issue(%s) not found in sprint(%s)"
		+ " in workspace(%s).";

	public IssueNotFoundException(String issueKey) {
		super(String.format(ISSUE_KEY_MESSAGE, issueKey));
	}

	public IssueNotFoundException(String issueKey, String workspaceCode) {
		super(String.format(ISSUE_KEY_CODE_MESSAGE, issueKey, workspaceCode));
	}

	public IssueNotFoundException(String issueKey, String sprintKey, String workspaceCode) {
		super(String.format(ISSUE_KEY_SPRINT_KEY_CODE_MESSAGE, issueKey, sprintKey, workspaceCode));
	}

	public IssueNotFoundException(String issueKey, String workspaceCode, Throwable cause) {
		super(String.format(ISSUE_KEY_CODE_MESSAGE, issueKey, workspaceCode), cause);
	}

	public IssueNotFoundException(String issueKey, String sprintKey, String workspaceCode, Throwable cause) {
		super(String.format(ISSUE_KEY_SPRINT_KEY_CODE_MESSAGE, issueKey, sprintKey, workspaceCode), cause);
	}
}
