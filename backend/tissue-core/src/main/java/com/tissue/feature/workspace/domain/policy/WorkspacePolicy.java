package com.tissue.feature.workspace.domain.policy;

import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.OwnerCannotLeaveWorkspaceException;
import com.tissue.feature.workspace.domain.exception.WorkspaceMemberLimitExceededException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkspacePolicy {

    private final int maxMembers;

    public void ensureCanAddMember(String workspaceKey, int currentCount) {
        if (currentCount >= maxMembers) {
            throw new WorkspaceMemberLimitExceededException(workspaceKey, maxMembers);
        }
    }

    public void ensureCanLeaveWorkspace(WorkspaceMember workspaceMember) {
        if (workspaceMember.getRole() == WorkspaceRole.OWNER) {
            throw new OwnerCannotLeaveWorkspaceException(workspaceMember);
        }
    }

    // TODO: ensureCanAddProject
    //  check for max number of projects a single workspace can have
}
