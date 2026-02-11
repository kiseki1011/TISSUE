package com.tissue.feature.member.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.EMAIL;
import static com.tissue.shared.exception.ErrorContextKeys.USERNAME;

import com.tissue.shared.exception.base.ResourceConflictException;

// TODO: tissue-security로 옮길까?
public class MemberSignupConflictException extends ResourceConflictException {

    public MemberSignupConflictException(String email, String username, Throwable cause) {
        super(MemberErrorCode.MEMBER_SIGNUP_CONFLICT, cause);
        addContext(EMAIL, email);
        addContext(USERNAME, username);
    }
}
