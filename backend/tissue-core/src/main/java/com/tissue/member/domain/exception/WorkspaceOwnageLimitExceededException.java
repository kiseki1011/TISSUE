package com.tissue.member.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.common.exception.ErrorContextKeys.USERNAME;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.member.domain.Member;

public class WorkspaceOwnageLimitExceededException extends BadRequestException {

    public WorkspaceOwnageLimitExceededException(Member member, int limit) {
        super(MemberErrorCode.WORKSPACE_OWNAGE_LIMIT_EXCEEDED);
        addContext(MEMBER_ID, member.getId());
        addContext(USERNAME, member.getUsername());
        addContext("workspaceCreateLimit", limit);
    }
}
