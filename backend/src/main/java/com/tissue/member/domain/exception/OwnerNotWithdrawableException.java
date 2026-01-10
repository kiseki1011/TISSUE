package com.tissue.member.domain.exception;

import static com.tissue.global.exception.ContextKeys.MEMBER_ID;
import static com.tissue.global.exception.ContextKeys.USERNAME;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.member.domain.Member;

public class OwnerNotWithdrawableException extends BadRequestException {

    public OwnerNotWithdrawableException(Member member) {
        super(MemberErrorCode.OWNER_NOT_WITHDRAWABLE);
        addContext(MEMBER_ID, member.getId());
        addContext(USERNAME, member.getUsername());
    }
}
