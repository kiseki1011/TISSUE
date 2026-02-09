package com.tissue.member.domain.exception;

import static com.tissue.exception.ErrorContextKeys.MEMBER_ID;

import com.tissue.exception.base.ResourceNotFoundException;

public class ActiveMemberNotFoundException extends ResourceNotFoundException {

    public ActiveMemberNotFoundException(Long memberId) {
        super(MemberErrorCode.ACTIVE_MEMBER_NOT_FOUND);
        addContext(MEMBER_ID, memberId);
    }
}
