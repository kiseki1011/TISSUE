package com.tissue.member.domain.exception;

import static com.tissue.exception.ErrorContextKeys.EMAIL;

import com.tissue.exception.base.ForbiddenException;

public class UnauthorizedDomainException extends ForbiddenException {

    public UnauthorizedDomainException(String email) {
        super(MemberErrorCode.UNAUTHORIZED_DOMAIN);
        addContext(EMAIL, email);
    }
}
