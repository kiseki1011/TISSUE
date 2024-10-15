package com.uranus.taskmanager.api.invitation.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.authentication.LoginMember;
import com.uranus.taskmanager.api.authentication.LoginRequired;
import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.invitation.dto.response.InvitationAcceptResponse;
import com.uranus.taskmanager.api.invitation.service.InvitationService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/invitations")
public class InvitationController {
	private final InvitationService invitationService;

	@LoginRequired
	@PostMapping("/{workspaceCode}/accept")
	public ApiResponse<InvitationAcceptResponse> acceptInvitation(@PathVariable String workspaceCode,
		@LoginMember LoginMemberDto loginMember) {

		InvitationAcceptResponse response = invitationService.acceptInvitation(loginMember, workspaceCode);
		return ApiResponse.ok("Invitation Accepted", response);
	}

	@LoginRequired
	@PostMapping("/{workspaceCode}/reject")
	public ApiResponse<Void> rejectInvitation(@PathVariable String workspaceCode,
		@LoginMember LoginMemberDto loginMember) {

		invitationService.rejectInvitation(loginMember, workspaceCode);
		return ApiResponse.ok("Invitation Rejected", null);
	}
}
