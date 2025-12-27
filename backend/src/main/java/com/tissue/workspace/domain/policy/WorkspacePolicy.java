package com.tissue.workspace.domain.policy;

import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.WorkspaceExceptions;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkspacePolicy {

	private final int maxMembers;

	public void ensureCanAddMember(String workspaceKey, int currentCount) {
		if (currentCount >= maxMembers) {
			throw WorkspaceExceptions.memberLimitExceeded(workspaceKey, maxMembers);
		}
	}

	public void ensureCanLeaveWorkspace(WorkspaceMember workspaceMember) {
		if (workspaceMember.getRole() == WorkspaceRole.OWNER) {
			throw WorkspaceExceptions.ownerCannotLeave(workspaceMember);
		}
	}

	// TODO: ensureCanAddProject
	//  check for max number of projects a single workspace can have
}
