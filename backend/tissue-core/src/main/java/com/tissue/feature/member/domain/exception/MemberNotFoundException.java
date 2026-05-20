package com.tissue.feature.member.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.EMAIL;
import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class MemberNotFoundException extends ResourceNotFoundException {

    public MemberNotFoundException(Long memberId) {
        super(MemberErrorCode.MEMBER_NOT_FOUND);
        addContext(MEMBER_ID, memberId);
    }

    public MemberNotFoundException(String email) {
        super(MemberErrorCode.MEMBER_NOT_FOUND);
        addContext(EMAIL, email);
    }
}
