package com.tissue.workspace.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.resolver.CurrentMember;
import com.tissue.workspace.adapter.in.web.dto.request.InviteToProjectRequest;
import com.tissue.workspace.adapter.in.web.dto.request.InviteToWorkspaceRequest;
import com.tissue.workspace.application.dto.in.KickWorkspaceMemberCommand;
import com.tissue.workspace.application.dto.in.LeaveWorkspaceCommand;
import com.tissue.workspace.application.dto.out.command.InviteMembersResponse;
import com.tissue.workspace.application.port.in.WorkspaceParticipationUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
public class WorkspaceParticipationController {

	private final WorkspaceParticipationUseCase workspaceParticipationUseCase;

	@PostMapping("/invitations")
	public ResponseEntity<InviteMembersResponse> inviteToWorkspace(
		@PathVariable String workspaceKey,
		@RequestBody @Valid InviteToWorkspaceRequest request
	) {
		var command = request.toCommand(workspaceKey);
		InviteMembersResponse response = workspaceParticipationUseCase.inviteToWorkspace(command);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/projects/{projectKey}/invitations")
	public ResponseEntity<InviteMembersResponse> inviteToProject(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestBody @Valid InviteToProjectRequest request
	) {
		var command = request.toCommand(workspaceKey, projectKey);
		InviteMembersResponse response = workspaceParticipationUseCase.inviteToProject(command);

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> leaveWorkspace(
		@PathVariable String workspaceKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		var command = new LeaveWorkspaceCommand(workspaceKey, userDetails.getMemberId());
		workspaceParticipationUseCase.leave(command);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{memberId}")
	public ResponseEntity<Void> kickWorkspaceMember(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		var command = new KickWorkspaceMemberCommand(workspaceKey, memberId, userDetails.getMemberId());
		workspaceParticipationUseCase.kick(command);

		return ResponseEntity.noContent().build();
	}
}
