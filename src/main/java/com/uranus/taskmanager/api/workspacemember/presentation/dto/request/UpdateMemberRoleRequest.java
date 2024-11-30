package com.uranus.taskmanager.api.workspacemember.presentation.dto.request;

import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateMemberRoleRequest {

	@NotBlank(message = "The member identifier must not be blank")
	private String memberIdentifier;

	@NotNull(message = "Select a valid workspace role")
	private WorkspaceRole updateWorkspaceRole;

	public UpdateMemberRoleRequest(String memberIdentifier, WorkspaceRole updateWorkspaceRole) {
		this.memberIdentifier = memberIdentifier;
		this.updateWorkspaceRole = updateWorkspaceRole;
	}
}
