package com.tissue.feature.member.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.EMAIL;
import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class ActiveMemberNotFoundException extends ResourceNotFoundException {

    public ActiveMemberNotFoundException(Long memberId) {
        super(MemberErrorCode.ACTIVE_MEMBER_NOT_FOUND);
        addContext(MEMBER_ID, memberId);
    }

    public ActiveMemberNotFoundException(String email) {
        super(MemberErrorCode.ACTIVE_MEMBER_NOT_FOUND);
        addContext(EMAIL, email);
    }
}
