package com.tissue.security.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.EMAIL;
import static com.tissue.shared.exception.ErrorContextKeys.USERNAME;

import com.tissue.shared.exception.base.ResourceConflictException;
import org.jspecify.annotations.Nullable;

public class MemberSignupConflictException extends ResourceConflictException {

    public MemberSignupConflictException(@Nullable String email, String username, Throwable cause) {
        super(AuthenticationErrorCode.MEMBER_SIGNUP_CONFLICT, cause);
        if (email != null) {
            addContext(EMAIL, email);
        }
        addContext(USERNAME, username);
    }
}
