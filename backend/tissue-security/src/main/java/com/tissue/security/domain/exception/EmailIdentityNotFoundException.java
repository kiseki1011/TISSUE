package com.tissue.security.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class EmailIdentityNotFoundException extends ResourceNotFoundException {

    public EmailIdentityNotFoundException(Long memberId) {
        super(AuthenticationErrorCode.EMAIL_AUTHENTICATION_IDENTITY_NOT_FOUND);
        addContext(MEMBER_ID, memberId);
    }
}
