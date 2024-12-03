package com.uranus.taskmanager.api.workspacemember.presentation.dto.request;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateMemberRoleRequest {

	@NotNull(message = "Select a valid workspace role")
	private WorkspaceRole updateWorkspaceRole;

	public UpdateMemberRoleRequest(WorkspaceRole updateWorkspaceRole) {
		this.updateWorkspaceRole = updateWorkspaceRole;
	}
}
