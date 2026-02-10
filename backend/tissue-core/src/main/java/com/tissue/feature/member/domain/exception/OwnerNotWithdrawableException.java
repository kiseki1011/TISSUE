package com.tissue.feature.member.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.shared.exception.ErrorContextKeys.USERNAME;

import com.tissue.feature.member.domain.Member;
import com.tissue.shared.exception.base.BadRequestException;

public class OwnerNotWithdrawableException extends BadRequestException {

    public OwnerNotWithdrawableException(Member member) {
        super(MemberErrorCode.OWNER_NOT_WITHDRAWABLE);
        addContext(MEMBER_ID, member.getId());
        addContext(USERNAME, member.getUsername());
    }
}
