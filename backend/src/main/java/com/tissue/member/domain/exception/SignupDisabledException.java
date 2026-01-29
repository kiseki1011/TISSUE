package com.tissue.member.domain.exception;

import com.tissue.common.exception.base.ForbiddenException;

public class SignupDisabledException extends ForbiddenException {

    public SignupDisabledException() {
        super(MemberErrorCode.EMAIL_SIGNUP_DISABLED);
    }
}
