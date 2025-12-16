package com.tissue.workspace.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.resolver.CurrentMember;
import com.tissue.workspace.adapter.in.web.dto.request.UpdateDisplayNameRequest;
import com.tissue.workspace.adapter.in.web.dto.request.UpdateRoleRequest;
import com.tissue.workspace.application.dto.request.AddPositionCommand;
import com.tissue.workspace.application.dto.request.AddTeamCommand;
import com.tissue.workspace.application.dto.request.RemovePositionCommand;
import com.tissue.workspace.application.dto.request.RemoveTeamCommand;
import com.tissue.workspace.application.dto.request.UpdateDisplayNameCommand;
import com.tissue.workspace.application.dto.request.UpdateRoleCommand;
import com.tissue.workspace.application.port.in.WorkspaceMemberManageUseCase;
import com.tissue.workspace.application.port.in.WorkspaceMemberQueryUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/members")
public class WorkspaceMemberController {

	private final WorkspaceMemberManageUseCase workspaceMemberManageUseCase;
	private final WorkspaceMemberQueryUseCase workspaceMemberQueryUseCase;

	@PatchMapping("/{memberId}/display-name")
	public ResponseEntity<Void> updateDisplayName(
		@PathVariable String workspaceKey,
		@RequestBody @Valid UpdateDisplayNameRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		workspaceMemberManageUseCase.updateDisplayName(
			new UpdateDisplayNameCommand(
				workspaceKey,
				userDetails.getMemberId(),
				request.displayName()
			)
		);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{memberId}/role")
	public ResponseEntity<Void> updateRole(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@RequestBody @Valid UpdateRoleRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		workspaceMemberManageUseCase.updateRole(
			new UpdateRoleCommand(
				workspaceKey,
				memberId,
				userDetails.getMemberId(),
				request.role()
			)
		);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{memberId}/positions/{positionId}")
	public ResponseEntity<Void> addPosition(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@PathVariable Long positionId,
		@CurrentMember MemberUserDetails userDetails
	) {
		workspaceMemberManageUseCase.addPosition(
			new AddPositionCommand(
				workspaceKey,
				memberId,
				userDetails.getMemberId(),
				positionId
			)
		);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{memberId}/positions/{positionId}")
	public ResponseEntity<Void> removePosition(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@PathVariable Long positionId,
		@CurrentMember MemberUserDetails userDetails
	) {
		workspaceMemberManageUseCase.removePosition(
			new RemovePositionCommand(
				workspaceKey,
				memberId,
				userDetails.getMemberId(),
				positionId
			)
		);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{memberId}/teams/{teamId}")
	public ResponseEntity<Void> addTeam(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@PathVariable Long teamId,
		@CurrentMember MemberUserDetails userDetails
	) {
		workspaceMemberManageUseCase.addTeam(
			new AddTeamCommand(
				workspaceKey,
				memberId,
				userDetails.getMemberId(),
				teamId
			)
		);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{memberId}/teams/{teamId}")
	public ResponseEntity<Void> removeTeam(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@PathVariable Long teamId,
		@CurrentMember MemberUserDetails userDetails
	) {
		workspaceMemberManageUseCase.removeTeam(
			new RemoveTeamCommand(
				workspaceKey,
				memberId,
				userDetails.getMemberId(),
				teamId
			)
		);
		return ResponseEntity.noContent().build();
	}
}

