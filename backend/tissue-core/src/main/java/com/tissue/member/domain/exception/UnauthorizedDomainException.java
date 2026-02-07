package com.tissue.member.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.EMAIL;

import com.tissue.common.exception.base.ForbiddenException;

public class UnauthorizedDomainException extends ForbiddenException {

    public UnauthorizedDomainException(String email) {
        super(MemberErrorCode.UNAUTHORIZED_DOMAIN);
        addContext(EMAIL, email);
    }
}
