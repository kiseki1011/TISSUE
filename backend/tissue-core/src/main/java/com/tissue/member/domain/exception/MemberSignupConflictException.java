package com.tissue.member.domain.exception;

import static com.tissue.exception.ErrorContextKeys.EMAIL;
import static com.tissue.exception.ErrorContextKeys.USERNAME;

import com.tissue.exception.base.ResourceConflictException;

public class MemberSignupConflictException extends ResourceConflictException {

    public MemberSignupConflictException(String email, String username, Throwable cause) {
        super(MemberErrorCode.MEMBER_SIGNUP_CONFLICT, cause);
        addContext(EMAIL, email);
        addContext(USERNAME, username);
    }
}
