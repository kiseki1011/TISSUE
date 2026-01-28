package com.tissue.member.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.common.exception.ErrorContextKeys.USERNAME;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.member.domain.Member;

public class OwnerNotWithdrawableException extends BadRequestException {

    public OwnerNotWithdrawableException(Member member) {
        super(MemberErrorCode.OWNER_NOT_WITHDRAWABLE);
        addContext(MEMBER_ID, member.getId());
        addContext(USERNAME, member.getUsername());
    }
}
