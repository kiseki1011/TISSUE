package com.tissue.security.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.EMAIL;
import static com.tissue.shared.exception.ErrorContextKeys.USERNAME;

import com.tissue.shared.exception.base.ResourceConflictException;

public class MemberSignupConflictException extends ResourceConflictException {

    public MemberSignupConflictException(String email, String username, Throwable cause) {
        super(AuthenticationErrorCode.MEMBER_SIGNUP_CONFLICT, cause);
        addContext(EMAIL, email);
        addContext(USERNAME, username);
    }

    public MemberSignupConflictException(String email, String message) {
        super(AuthenticationErrorCode.MEMBER_SIGNUP_CONFLICT);
        addContext(EMAIL, email);
        addContext("reason", message);
    }
}
