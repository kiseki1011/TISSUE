package com.tissue.api.workspacemember.presentation.controller.command;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.application.dto.RemoveWorkspaceMemberCommand;
import com.tissue.api.workspacemember.application.dto.TransferOwnershipCommand;
import com.tissue.api.workspacemember.application.dto.UpdateRoleCommand;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberInviteService;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberService;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateRoleRequest;
import com.tissue.api.workspacemember.presentation.dto.response.InviteMembersResponse;
import com.tissue.api.workspacemember.presentation.dto.response.WorkspaceMemberResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/members")
public class WorkspaceMembershipController {

	/**
	 * TODO
	 *  - Leave Workspace API is in WorkspaceParticipationController
	 *  - Should I integrate WorkspaceMembershipController to WorkspaceParticipationController?
	 */

	private final WorkspaceMemberService workspaceMemberService;
	private final WorkspaceMemberInviteService workspaceMemberInviteService;

	// TODO: Where should I place inviteMembers API?
	//  WorkspaceMembershipController or InvitationController? Or some other controller?
	//  Inviting a member only creates the invitation.
	//  The Member joins the Workspace(is made into a WorkspaceMember) when the Invitation is accepted.
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/invite")
	public ApiResponse<InviteMembersResponse> inviteMembers(
		@PathVariable String workspaceKey,
		@RequestBody @Valid InviteMembersRequest request
	) {
		InviteMembersResponse response = workspaceMemberInviteService.inviteMembers(
			workspaceKey,
			request
		);

		return ApiResponse.ok("Members invited", response);
	}

	@RoleRequired(role = WorkspaceRole.ADMIN)
	@PatchMapping("/{memberId}/role")
	public ApiResponse<WorkspaceMemberResponse> updateRole(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@RequestBody @Valid UpdateRoleRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberResponse response = workspaceMemberService.updateRole(
			new UpdateRoleCommand(workspaceKey, memberId, userDetails.getMemberId(), request.role())
		);

		return ApiResponse.ok("Member workspace role updated.", response);
	}

	@RoleRequired(role = WorkspaceRole.OWNER)
	@PatchMapping("/{memberId}/ownership")
	public ApiResponse<WorkspaceMemberResponse> transferWorkspaceOwnership(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		WorkspaceMemberResponse response = workspaceMemberService.transferOwnership(
			new TransferOwnershipCommand(workspaceKey, memberId, userDetails.getMemberId())
		);

		return ApiResponse.ok("Ownership transferred.", response);
	}

	// TODO: User ResponseEntity for HttpStatus.NO_CONTENT
	@RoleRequired(role = WorkspaceRole.ADMIN)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/{memberId}")
	public ApiResponse<Void> removeWorkspaceMember(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		workspaceMemberService.removeWorkspaceMember(
			new RemoveWorkspaceMemberCommand(workspaceKey, memberId, userDetails.getMemberId())
		);

		return ApiResponse.okWithNoContent("Member removed from workspace.");
	}
}
