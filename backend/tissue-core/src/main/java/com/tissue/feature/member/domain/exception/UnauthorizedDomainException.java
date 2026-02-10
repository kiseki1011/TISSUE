package com.tissue.feature.member.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.EMAIL;

import com.tissue.shared.exception.base.ForbiddenException;

public class UnauthorizedDomainException extends ForbiddenException {

    public UnauthorizedDomainException(String email) {
        super(MemberErrorCode.UNAUTHORIZED_DOMAIN);
        addContext(EMAIL, email);
    }
}
