package com.tissue.member.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.MEMBER_ID;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class ActiveMemberNotFoundException extends ResourceNotFoundException {

    public ActiveMemberNotFoundException(Long memberId) {
        super(MemberErrorCode.ACTIVE_MEMBER_NOT_FOUND);
        addContext(MEMBER_ID, memberId);
    }
}
