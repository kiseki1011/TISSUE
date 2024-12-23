package com.tissue.api.workspacemember.presentation.dto.request;

import com.tissue.api.workspacemember.domain.WorkspaceRole;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateRoleRequest {

	@NotNull(message = "Select a valid workspace role")
	private WorkspaceRole updateWorkspaceRole;

	public UpdateRoleRequest(WorkspaceRole updateWorkspaceRole) {
		this.updateWorkspaceRole = updateWorkspaceRole;
	}
}
