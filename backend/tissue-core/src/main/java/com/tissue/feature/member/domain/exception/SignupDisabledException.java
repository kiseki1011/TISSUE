package com.tissue.feature.member.domain.exception;

import com.tissue.shared.exception.base.ForbiddenException;

public class SignupDisabledException extends ForbiddenException {

    public SignupDisabledException() {
        super(MemberErrorCode.EMAIL_SIGNUP_DISABLED);
    }
}
