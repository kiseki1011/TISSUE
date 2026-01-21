package com.tissue.member.domain.exception;

import static com.tissue.global.exception.ContextKeys.USERNAME;

import com.tissue.global.exception.base.ResourceConflictException;

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
