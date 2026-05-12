package com.tissue.security.domain.exception;

import com.tissue.shared.exception.base.ForbiddenException;

public class SignupBlockedNoWorkspaceException extends ForbiddenException {

    public SignupBlockedNoWorkspaceException() {
        super(AuthenticationErrorCode.SIGNUP_BLOCKED_NO_WORKSPACE);
    }
}
