package com.tissue.api.workspace.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspace.adapter.in.web.dto.request.UpdateDisplayNameRequest;
import com.tissue.api.workspace.adapter.in.web.dto.request.UpdateRoleRequest;
import com.tissue.api.workspace.application.dto.request.AddPositionCommand;
import com.tissue.api.workspace.application.dto.request.AddTeamCommand;
import com.tissue.api.workspace.application.dto.request.KickWorkspaceMemberCommand;
import com.tissue.api.workspace.application.dto.request.RemovePositionCommand;
import com.tissue.api.workspace.application.dto.request.RemoveTeamCommand;
import com.tissue.api.workspace.application.dto.request.UpdateDisplayNameCommand;
import com.tissue.api.workspace.application.dto.request.UpdateRoleCommand;
import com.tissue.api.workspace.application.dto.response.WorkspaceCommandResult;
import com.tissue.api.workspace.application.dto.response.WorkspaceMemberCommandResult;
import com.tissue.api.workspace.application.port.in.WorkspaceMemberCommandUseCase;
import com.tissue.api.workspace.application.port.in.WorkspaceParticipationUseCase;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/members")
public class WorkspaceMemberController {

	private final WorkspaceMemberCommandUseCase workspaceMemberCommandUseCase;
	private final WorkspaceParticipationUseCase workspaceParticipationUseCase;

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{memberId}/display-name")
	public ResponseEntity<WorkspaceMemberCommandResult> updateDisplayName(
		@PathVariable String workspaceKey,
		@RequestBody @Valid UpdateDisplayNameRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberCommandResult response = workspaceMemberCommandUseCase.updateDisplayName(
			new UpdateDisplayNameCommand(
				workspaceKey,
				userDetails.getMemberId(),
				request.displayName()
			)
		);

		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.ADMIN)
	@PatchMapping("/{memberId}/role")
	public ResponseEntity<WorkspaceMemberCommandResult> updateRole(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@RequestBody @Valid UpdateRoleRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberCommandResult response = workspaceMemberCommandUseCase.updateRole(
			new UpdateRoleCommand(
				workspaceKey,
				memberId,
				userDetails.getMemberId(),
				request.role()
			)
		);

		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{memberId}/positions/{positionId}")
	public ResponseEntity<WorkspaceMemberCommandResult> addPosition(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@PathVariable Long positionId,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberCommandResult response = workspaceMemberCommandUseCase.addPosition(
			new AddPositionCommand(
				workspaceKey,
				memberId,
				userDetails.getMemberId(),
				positionId
			)
		);

		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{memberId}/positions/{positionId}")
	public ResponseEntity<WorkspaceMemberCommandResult> removePosition(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@PathVariable Long positionId,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberCommandResult response = workspaceMemberCommandUseCase.removePosition(
			new RemovePositionCommand(
				workspaceKey,
				memberId,
				userDetails.getMemberId(),
				positionId
			)
		);

		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{memberId}/teams/{teamId}")
	public ResponseEntity<WorkspaceMemberCommandResult> addTeam(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@PathVariable Long teamId,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberCommandResult response = workspaceMemberCommandUseCase.addTeam(
			new AddTeamCommand(
				workspaceKey,
				memberId,
				userDetails.getMemberId(),
				teamId
			)
		);

		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{memberId}/teams/{teamId}")
	public ResponseEntity<WorkspaceMemberCommandResult> removeTeam(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@PathVariable Long teamId,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberCommandResult response = workspaceMemberCommandUseCase.removeTeam(
			new RemoveTeamCommand(
				workspaceKey,
				memberId,
				userDetails.getMemberId(),
				teamId
			)
		);

		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping
	public ResponseEntity<WorkspaceCommandResult> leaveWorkspace(
		@PathVariable String workspaceKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceCommandResult response = workspaceParticipationUseCase.leave(
			workspaceKey,
			userDetails.getMemberId()
		);
		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.ADMIN)
	@DeleteMapping("/{memberId}")
	public ResponseEntity<WorkspaceMemberCommandResult> kickWorkspaceMember(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberCommandResult response = workspaceParticipationUseCase.kick(
			new KickWorkspaceMemberCommand(
				workspaceKey,
				memberId,
				userDetails.getMemberId()
			)
		);
		return ResponseEntity.ok(response);
	}
}
