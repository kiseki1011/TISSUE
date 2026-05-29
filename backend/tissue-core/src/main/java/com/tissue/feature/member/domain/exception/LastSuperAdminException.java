package com.tissue.feature.member.domain.exception;

import com.tissue.shared.exception.base.ResourceConflictException;

public class LastSuperAdminException extends ResourceConflictException {

    public LastSuperAdminException() {
        super(MemberErrorCode.LAST_SUPER_ADMIN);
    }
}
