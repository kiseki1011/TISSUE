package com.tissue.member.domain.exception;

import static com.tissue.global.exception.ContextKeys.EMAIL;

import com.tissue.global.exception.base.ResourceConflictException;

public class DuplicateEmailException extends ResourceConflictException {

    public DuplicateEmailException(String email) {
        super(MemberErrorCode.DUPLICATE_EMAIL);
        addContext(EMAIL, email);
    }

    public DuplicateEmailException(String email, Throwable cause) {
        super(MemberErrorCode.DUPLICATE_EMAIL, cause);
        addContext(EMAIL, email);
    }
}
