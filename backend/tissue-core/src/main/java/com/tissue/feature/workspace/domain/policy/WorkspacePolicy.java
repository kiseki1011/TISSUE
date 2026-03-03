package com.tissue.feature.workspace.domain.policy;

import static com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode.OWNER_CANNOT_LEAVE_WORKSPACE;
import static com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode.WORKSPACE_MEMBER_LIMIT_EXCEEDED;
import static com.tissue.shared.exception.ErrorContextKeys.MAX_WORKSPACE_MEMBER;

import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.BadRequestException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkspacePolicy {

    private final int maxMembers;

    public void ensureCanAddMember(int currentCount) {
        if (currentCount >= maxMembers) {
            throw new BadRequestException(WORKSPACE_MEMBER_LIMIT_EXCEEDED).addContext(MAX_WORKSPACE_MEMBER, maxMembers);
        }
    }

    public void ensureCanLeaveWorkspace(WorkspaceMember workspaceMember) {
        if (workspaceMember.getRole() == WorkspaceRole.OWNER) {
            throw new BadRequestException(OWNER_CANNOT_LEAVE_WORKSPACE);
        }
    }

    // TODO: ensureCanAddProject
    //  check for max number of projects a single workspace can have
}
