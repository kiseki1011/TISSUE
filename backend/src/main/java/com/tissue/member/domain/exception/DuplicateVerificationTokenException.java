package com.tissue.member.domain.exception;

import static com.tissue.global.exception.ContextKeys.EMAIL;

import com.tissue.global.exception.base.ResourceConflictException;

public class DuplicateVerificationTokenException extends ResourceConflictException {

    public DuplicateVerificationTokenException(String email) {
        super(MemberErrorCode.VERIFICATION_TOKEN_DUPLICATE);
        addContext(EMAIL, email);
    }

    public DuplicateVerificationTokenException(String email, Throwable cause) {
        super(MemberErrorCode.VERIFICATION_TOKEN_DUPLICATE, cause);
        addContext(EMAIL, email);
    }
}
