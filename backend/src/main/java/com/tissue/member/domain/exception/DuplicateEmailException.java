package com.tissue.member.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.EMAIL;

import com.tissue.common.exception.base.ResourceConflictException;

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
