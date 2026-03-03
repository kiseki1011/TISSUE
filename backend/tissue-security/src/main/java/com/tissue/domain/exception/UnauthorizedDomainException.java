package com.tissue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.EMAIL;

import com.tissue.shared.exception.base.ForbiddenException;

public class UnauthorizedDomainException extends ForbiddenException {

    public UnauthorizedDomainException(String email) {
        super(AuthenticationErrorCode.UNAUTHORIZED_DOMAIN);
        addContext(EMAIL, email);
    }
}
