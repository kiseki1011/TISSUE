package com.tissue.api.workspace.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspace.adapter.in.web.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.adapter.in.web.dto.request.InviteMembersRequest;
import com.tissue.api.workspace.adapter.in.web.dto.request.UpdateWorkspaceInfoRequest;
import com.tissue.api.workspace.application.dto.request.DeleteWorkspaceCommand;
import com.tissue.api.workspace.application.dto.request.InviteMembersCommand;
import com.tissue.api.workspace.application.dto.request.TransferOwnershipCommand;
import com.tissue.api.workspace.application.dto.response.InviteMembersResult;
import com.tissue.api.workspace.application.dto.response.WorkspaceCommandResult;
import com.tissue.api.workspace.application.dto.response.WorkspaceDetail;
import com.tissue.api.workspace.application.port.in.WorkspaceCommandUseCase;
import com.tissue.api.workspace.application.port.in.WorkspaceCreateUseCase;
import com.tissue.api.workspace.application.port.in.WorkspaceParticipationUseCase;
import com.tissue.api.workspace.application.port.in.WorkspaceQueryUseCase;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

	private final WorkspaceCreateUseCase workspaceCreateUseCase;
	private final WorkspaceCommandUseCase workspaceCommandUseCase;
	private final WorkspaceParticipationUseCase workspaceParticipationUseCase;
	private final WorkspaceQueryUseCase workspaceQueryUseCase;

	@PostMapping
	public ResponseEntity<WorkspaceCommandResult> create(
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid CreateWorkspaceRequest request
	) {
		WorkspaceCommandResult response = workspaceCreateUseCase.create(
			request.toCommand(userDetails.getMemberId())
		);

		// TODO: ResponseEntity.created 사용법?
		// return ResponseEntity.created();

		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(response);
	}

	@RoleRequired(role = WorkspaceRole.ADMIN)
	@PatchMapping("/{workspaceKey}/info")
	public ResponseEntity<WorkspaceCommandResult> updateInfo(
		@PathVariable String workspaceKey,
		@RequestBody @Valid UpdateWorkspaceInfoRequest request
	) {
		WorkspaceCommandResult response = workspaceCommandUseCase.updateInfo(request.toCommand(workspaceKey));
		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.OWNER)
	@DeleteMapping("/{workspaceKey}")
	public ResponseEntity<WorkspaceCommandResult> delete(
		@PathVariable String workspaceKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceCommandResult response = workspaceCommandUseCase.delete(new DeleteWorkspaceCommand(workspaceKey));
		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.OWNER)
	@PatchMapping("/{workspaceKey}/members/{memberId}/ownership")
	public ResponseEntity<WorkspaceCommandResult> transferOwnership(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceCommandResult response = workspaceCommandUseCase.transferOwnership(
			new TransferOwnershipCommand(
				workspaceKey,
				userDetails.getMemberId(),
				memberId
			)
		);
		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/invite")
	public ResponseEntity<InviteMembersResult> inviteMembers(
		@PathVariable String workspaceKey,
		@RequestBody @Valid InviteMembersRequest request
	) {
		InviteMembersResult response = workspaceParticipationUseCase.invite(
			new InviteMembersCommand(
				workspaceKey,
				request.emails()
			)
		);
		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@GetMapping("/{workspaceKey}")
	public ResponseEntity<WorkspaceDetail> getDetail(
		@PathVariable String workspaceKey
	) {
		WorkspaceDetail response = workspaceQueryUseCase.getDetail(workspaceKey);
		return ResponseEntity.ok(response);
	}
}
