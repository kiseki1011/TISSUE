package com.tissue.feature.member.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.EMAIL;

import com.tissue.shared.exception.base.ResourceConflictException;

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
