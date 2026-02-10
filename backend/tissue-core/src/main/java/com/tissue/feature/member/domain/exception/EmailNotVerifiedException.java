package com.tissue.feature.member.domain.exception;

import static com.tissue.global.security.exception.AuthenticationErrorCode.EMAIL_NOT_VERIFIED;
import static com.tissue.shared.exception.ErrorContextKeys.EMAIL;

import com.tissue.shared.exception.base.ForbiddenException;

public class EmailNotVerifiedException extends ForbiddenException {

    public EmailNotVerifiedException(String email) {
        super(EMAIL_NOT_VERIFIED);
        addContext(EMAIL, email);
    }
}
