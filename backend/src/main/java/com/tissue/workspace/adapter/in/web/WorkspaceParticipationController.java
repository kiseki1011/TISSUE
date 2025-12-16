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
import com.tissue.workspace.application.dto.request.KickWorkspaceMemberCommand;
import com.tissue.workspace.application.dto.response.InviteMembersResponse;
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
		InviteMembersResponse response = workspaceParticipationUseCase.inviteToWorkspace(
			request.toCommand(workspaceKey));
		return ResponseEntity.ok(response);
	}

	@PostMapping("/projects/{projectKey}/invitations")
	public ResponseEntity<InviteMembersResponse> inviteToProject(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestBody @Valid InviteToProjectRequest request
	) {
		InviteMembersResponse response = workspaceParticipationUseCase.inviteToProject(
			request.toCommand(workspaceKey, projectKey));
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> leaveWorkspace(
		@PathVariable String workspaceKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		workspaceParticipationUseCase.leave(workspaceKey, userDetails.getMemberId());
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{memberId}")
	public ResponseEntity<Void> kickWorkspaceMember(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		workspaceParticipationUseCase.kick(
			new KickWorkspaceMemberCommand(workspaceKey, memberId, userDetails.getMemberId()));
		return ResponseEntity.noContent().build();
	}
}
