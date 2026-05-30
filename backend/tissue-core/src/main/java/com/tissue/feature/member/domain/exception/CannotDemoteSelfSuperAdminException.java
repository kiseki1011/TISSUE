package com.tissue.feature.member.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;

import com.tissue.shared.exception.base.ResourceConflictException;

public class CannotDemoteSelfSuperAdminException extends ResourceConflictException {

    public CannotDemoteSelfSuperAdminException(Long memberId) {
        super(MemberErrorCode.CANNOT_DEMOTE_SELF_SUPER_ADMIN);
        addContext(MEMBER_ID, memberId);
    }
}
