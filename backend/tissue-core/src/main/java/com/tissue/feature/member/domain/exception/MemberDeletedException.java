package com.tissue.feature.member.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class MemberDeletedException extends ResourceNotFoundException {

    public MemberDeletedException(Long memberId) {
        super(MemberErrorCode.MEMBER_DELETED);
        addContext(MEMBER_ID, memberId);
    }
}
