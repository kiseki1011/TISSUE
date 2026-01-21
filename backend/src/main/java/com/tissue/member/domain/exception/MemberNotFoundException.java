package com.tissue.member.domain.exception;

import static com.tissue.global.exception.ContextKeys.MEMBER_ID;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class MemberNotFoundException extends ResourceNotFoundException {

    public MemberNotFoundException(Long memberId) {
        super(MemberErrorCode.MEMBER_NOT_FOUND);
        addContext(MEMBER_ID, memberId);
    }
}
