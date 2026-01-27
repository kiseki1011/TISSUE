package com.tissue.member.domain.exception;

import static com.tissue.global.exception.ContextKeys.EMAIL;

import com.tissue.global.exception.base.ForbiddenException;

public class UnauthorizedDomainException extends ForbiddenException {

    public UnauthorizedDomainException(String email) {
        super(MemberErrorCode.UNAUTHORIZED_DOMAIN);
        addContext(EMAIL, email);
    }
}
