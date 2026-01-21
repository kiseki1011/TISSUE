package com.tissue.member.domain.exception;

import static com.tissue.global.exception.ContextKeys.MEMBER_ID;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class ActiveMemberNotFoundException extends ResourceNotFoundException {

    public ActiveMemberNotFoundException(Long memberId) {
        super(MemberErrorCode.ACTIVE_MEMBER_NOT_FOUND);
        addContext(MEMBER_ID, memberId);
    }
}
