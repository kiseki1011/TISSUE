package com.tissue.member.domain.exception;

import static com.tissue.exception.ErrorContextKeys.MEMBER_ID;

import com.tissue.exception.base.ResourceNotFoundException;

public class MemberNotFoundException extends ResourceNotFoundException {

    public MemberNotFoundException(Long memberId) {
        super(MemberErrorCode.MEMBER_NOT_FOUND);
        addContext(MEMBER_ID, memberId);
    }
}
