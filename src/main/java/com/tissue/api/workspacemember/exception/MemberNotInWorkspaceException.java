package com.tissue.api.workspacemember.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.WorkspaceMemberException;

public class MemberNotInWorkspaceException extends WorkspaceMemberException {
	private static final String MESSAGE = "Member was not found in this workspace";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public MemberNotInWorkspaceException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public MemberNotInWorkspaceException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
