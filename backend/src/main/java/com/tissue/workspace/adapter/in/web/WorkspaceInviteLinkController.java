package com.tissue.workspace.adapter.in.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.resolver.CurrentMember;
import com.tissue.workspace.adapter.in.web.dto.request.CreateProjectInviteLinkRequest;
import com.tissue.workspace.adapter.in.web.dto.request.CreateWorkspaceInviteLinkRequest;
import com.tissue.workspace.application.dto.request.ExpireLinkCommand;
import com.tissue.workspace.application.dto.request.JoinViaLinkCommand;
import com.tissue.workspace.application.dto.response.InviteLinkResponse;
import com.tissue.workspace.application.dto.response.WorkspaceMemberCommandResponse;
import com.tissue.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;
import com.tissue.workspace.application.port.in.WorkspaceInviteLinkUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/invite-links")
public class WorkspaceInviteLinkController {

	private final WorkspaceInviteLinkUseCase inviteLinkUseCase;

	@PostMapping
	public ResponseEntity<InviteLinkResponse> createWorkspaceLink(
		@PathVariable String workspaceKey,
		@RequestBody @Valid CreateWorkspaceInviteLinkRequest request
	) {
		String token = inviteLinkUseCase.createWorkspaceLink(request.toCommand(workspaceKey));

		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
			.path("/api/v1/workspaces/{workspaceKey}/invite-links/{token}/join")
			.buildAndExpand(workspaceKey, token)
			.toUri();

		return ResponseEntity.created(location)
			.body(new InviteLinkResponse(token, location.toString(), request.expiredAt()));
	}

	@PostMapping("/projects/{projectKey}")
	public ResponseEntity<InviteLinkResponse> createProjectLink(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestBody @Valid CreateProjectInviteLinkRequest request
	) {
		String token = inviteLinkUseCase.createProjectLink(request.toCommand(workspaceKey, projectKey));

		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
			.path("/api/v1/workspaces/{workspaceKey}/invite-links/{token}/join")
			.buildAndExpand(workspaceKey, token)
			.toUri();

		return ResponseEntity.created(location)
			.body(new InviteLinkResponse(token, location.toString(), request.expiredAt()));
	}

	@DeleteMapping("/{token}")
	public ResponseEntity<Void> expireLink(
		@PathVariable String workspaceKey,
		@PathVariable String token
	) {
		inviteLinkUseCase.expireLink(new ExpireLinkCommand(workspaceKey, token));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{token}/join")
	public ResponseEntity<WorkspaceMemberCommandResponse> joinViaLink(
		@PathVariable String workspaceKey,
		@PathVariable String token,
		@CurrentMember MemberUserDetails userDetails
	) {
		var command = new JoinViaLinkCommand(workspaceKey, token, userDetails.getMemberId());
		WorkspaceMemberCommandResponse response = inviteLinkUseCase.joinViaLink(command);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{token}")
	public ResponseEntity<WorkspaceInviteLinkDetail> getLinkInfo(
		@PathVariable String workspaceKey,
		@PathVariable String token
	) {
		WorkspaceInviteLinkDetail response = inviteLinkUseCase.getLinkInfo(workspaceKey, token);
		return ResponseEntity.ok(response);
	}

}
