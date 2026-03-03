package com.tissue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.EMAIL;

import com.tissue.shared.exception.base.ResourceConflictException;

public class DuplicateVerificationTokenException extends ResourceConflictException {

    public DuplicateVerificationTokenException(String email, Throwable cause) {
        super(AuthenticationErrorCode.VERIFICATION_TOKEN_DUPLICATE, cause);
        addContext(EMAIL, email);
    }
}
