package com.tissue.feature.workspace.domain.policy;

import static com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode.OWNER_CANNOT_LEAVE_WORKSPACE;
import static com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode.WORKSPACE_MEMBER_LIMIT_EXCEEDED;
import static com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode.WORKSPACE_PROJECT_LIMIT_EXCEEDED;
import static com.tissue.shared.exception.ErrorContextKeys.MAX_WORKSPACE_MEMBER;
import static com.tissue.shared.exception.ErrorContextKeys.MAX_WORKSPACE_PROJECT;

import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.BadRequestException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkspacePolicy {

    private final int maxMembers;
    private final int maxProjects;

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

    public void ensureCanAddProject(int currentCount) {
        if (currentCount >= maxProjects) {
            throw new BadRequestException(WORKSPACE_PROJECT_LIMIT_EXCEEDED)
                    .addContext(MAX_WORKSPACE_PROJECT, maxProjects);
        }
    }
}
