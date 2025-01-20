package com.tissue.api.issue.exception;

import com.tissue.api.common.exception.type.ResourceNotFoundException;

public class IssueNotFoundException extends ResourceNotFoundException {

	private static final String KEY_MESSAGE = "Issue not found with key: %s";
	private static final String KEY_CODE_MESSAGE = "Issue not found with key %s in workspace %s";

	public IssueNotFoundException(String issueKey) {
		super(String.format(KEY_MESSAGE, issueKey));
	}

	public IssueNotFoundException(String issueKey, String workspaceCode) {
		super(String.format(KEY_CODE_MESSAGE, issueKey, workspaceCode));
	}
}
