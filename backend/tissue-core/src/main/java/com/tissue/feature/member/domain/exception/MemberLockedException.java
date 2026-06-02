package com.tissue.feature.member.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;

import com.tissue.shared.exception.base.ForbiddenException;

public class MemberLockedException extends ForbiddenException {

    public MemberLockedException(Long memberId) {
        super(MemberErrorCode.MEMBER_LOCKED);
        addContext(MEMBER_ID, memberId);
    }
}
