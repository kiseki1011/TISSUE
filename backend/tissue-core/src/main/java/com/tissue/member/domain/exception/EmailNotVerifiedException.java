package com.tissue.member.domain.exception;

import static com.tissue.exception.ErrorContextKeys.EMAIL;
import static com.tissue.global.security.exception.AuthenticationErrorCode.EMAIL_NOT_VERIFIED;

import com.tissue.exception.base.ForbiddenException;

public class EmailNotVerifiedException extends ForbiddenException {

    public EmailNotVerifiedException(String email) {
        super(EMAIL_NOT_VERIFIED);
        addContext(EMAIL, email);
    }
}
