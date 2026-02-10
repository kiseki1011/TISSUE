package com.tissue.feature.member.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.shared.exception.ErrorContextKeys.USERNAME;

import com.tissue.feature.member.domain.Member;
import com.tissue.shared.exception.base.BadRequestException;

public class WorkspaceOwnageLimitExceededException extends BadRequestException {

    public WorkspaceOwnageLimitExceededException(Member member, int limit) {
        super(MemberErrorCode.WORKSPACE_OWNAGE_LIMIT_EXCEEDED);
        addContext(MEMBER_ID, member.getId());
        addContext(USERNAME, member.getUsername());
        addContext("workspaceCreateLimit", limit);
    }
}
