package com.tissue.member.domain.exception;

import static com.tissue.global.exception.ContextKeys.EMAIL;
import static com.tissue.security.authentication.domain.exception.AuthenticationErrorCode.EMAIL_NOT_VERIFIED;

import com.tissue.global.exception.base.ForbiddenException;

public class EmailNotVerifiedException extends ForbiddenException {

    public EmailNotVerifiedException(String email) {
        super(EMAIL_NOT_VERIFIED);
        addContext(EMAIL, email);
    }
}
