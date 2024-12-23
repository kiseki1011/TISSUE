package com.tissue.api.security.authorization.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.AuthorizationException;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

/**
 * Todo: 예외를 던질때 필요한 권한을 같이 넘겨서 권한 별로 매세지를 다르게 설정한다
 * 예시: ADMIN privileges are needed to access this resource
 */
public class InsufficientWorkspaceRoleException extends AuthorizationException {
	private static final String MESSAGE = "Access denied. Please check your permissions.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.FORBIDDEN;

	public InsufficientWorkspaceRoleException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public InsufficientWorkspaceRoleException(WorkspaceRole role) {
		super(MESSAGE + " You must be at least " + role + ".", HTTP_STATUS);
	}

	public InsufficientWorkspaceRoleException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}

}
