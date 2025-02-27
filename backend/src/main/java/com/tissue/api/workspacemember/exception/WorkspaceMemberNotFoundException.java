package com.tissue.api.workspacemember.exception;

import com.tissue.api.common.exception.type.ResourceNotFoundException;

public class WorkspaceMemberNotFoundException extends ResourceNotFoundException {

	private static final String ID_MESSAGE = "Workspace member not found with id: %d";
	private static final String ID_CODE_MESSAGE = "Workspace member with id %d was not found in workspace %s";

	public WorkspaceMemberNotFoundException(String message) {
		super(message);
	}

	public WorkspaceMemberNotFoundException(Long id) {
		super(String.format(ID_MESSAGE, id));
	}

	public WorkspaceMemberNotFoundException(Long id, String workspaceCode) {
		super(String.format(ID_CODE_MESSAGE, id, workspaceCode));
	}
}
