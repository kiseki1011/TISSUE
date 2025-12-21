package com.tissue.workspace.domain.policy;

import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.WorkspaceMemberLimitExceededException;

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
			throw new RuntimeException("Cannot leave workspace if workspace role is OWNER.");
		}
	}

	// TODO: ensureCanAddProject
	//  check for max number of projects a single workspace can have
}
