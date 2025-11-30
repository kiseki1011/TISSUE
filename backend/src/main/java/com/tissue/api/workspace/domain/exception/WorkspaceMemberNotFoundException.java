package com.tissue.api.workspace.domain.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

// TODO: 순서 리팩토링 workspace key -> member id
public class WorkspaceMemberNotFoundException extends ResourceNotFoundException {

	private static final String MESSAGE = "Cannot find workspace member with member id '%d' and workspace key '%s'.";

	public WorkspaceMemberNotFoundException(Long memberId, String workspaceKey) {
		super(MESSAGE.formatted(memberId, workspaceKey));

		addContext("workspaceKey", workspaceKey);
		addContext("memberId", memberId);
	}
}
