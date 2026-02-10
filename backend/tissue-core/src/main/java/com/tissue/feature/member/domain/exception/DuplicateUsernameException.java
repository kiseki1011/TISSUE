package com.tissue.feature.member.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.USERNAME;

import com.tissue.shared.exception.base.ResourceConflictException;

public class DuplicateUsernameException extends ResourceConflictException {

    public DuplicateUsernameException(String username) {
        super(MemberErrorCode.DUPLICATE_USERNAME);
        addContext(USERNAME, username);
    }

    public DuplicateUsernameException(String username, Throwable cause) {
        super(MemberErrorCode.DUPLICATE_USERNAME, cause);
        addContext(USERNAME, username);
    }
}
