package com.tissue.feature.member.domain.exception;

import com.tissue.shared.exception.base.ForbiddenException;

// TODO: tissue-security로 옮길까?
public class SignupDisabledException extends ForbiddenException {

    public SignupDisabledException() {
        super(MemberErrorCode.EMAIL_SIGNUP_DISABLED);
    }
}
