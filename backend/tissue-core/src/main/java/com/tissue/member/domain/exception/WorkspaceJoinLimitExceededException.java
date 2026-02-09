package com.tissue.member.domain.exception;

import static com.tissue.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.exception.ErrorContextKeys.USERNAME;

import com.tissue.exception.base.BadRequestException;
import com.tissue.member.domain.Member;

public class WorkspaceJoinLimitExceededException extends BadRequestException {

    public WorkspaceJoinLimitExceededException(Member member, int limit) {
        super(MemberErrorCode.WORKSPACE_JOIN_LIMIT_EXCEEDED);
        addContext(MEMBER_ID, member.getId());
        addContext(USERNAME, member.getUsername());
        addContext("workspaceJoinLimit", limit);
    }
}
