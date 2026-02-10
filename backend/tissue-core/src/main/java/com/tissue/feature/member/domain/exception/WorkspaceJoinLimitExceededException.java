package com.tissue.feature.member.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.shared.exception.ErrorContextKeys.USERNAME;

import com.tissue.feature.member.domain.Member;
import com.tissue.shared.exception.base.BadRequestException;

public class WorkspaceJoinLimitExceededException extends BadRequestException {

    public WorkspaceJoinLimitExceededException(Member member, int limit) {
        super(MemberErrorCode.WORKSPACE_JOIN_LIMIT_EXCEEDED);
        addContext(MEMBER_ID, member.getId());
        addContext(USERNAME, member.getUsername());
        addContext("workspaceJoinLimit", limit);
    }
}
