package com.uranus.taskmanager.api.invitation.presentation.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.invitation.presentation.dto.response.InvitationAcceptResponse;
import com.uranus.taskmanager.api.invitation.service.InvitationService;
import com.uranus.taskmanager.api.security.authentication.interceptor.LoginRequired;
import com.uranus.taskmanager.api.security.authentication.presentation.dto.LoginMember;
import com.uranus.taskmanager.api.security.authentication.resolver.ResolveLoginMember;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/invitations")
public class InvitationController {
	private final InvitationService invitationService;

	@LoginRequired
	@PostMapping("/{workspaceCode}/accept")
	public ApiResponse<InvitationAcceptResponse> acceptInvitation(@PathVariable String workspaceCode,
		@ResolveLoginMember LoginMember loginMember) {

		InvitationAcceptResponse response = invitationService.acceptInvitation(loginMember.getId(), workspaceCode);
		return ApiResponse.ok("Invitation Accepted", response);
	}

	@LoginRequired
	@PostMapping("/{workspaceCode}/reject")
	public ApiResponse<Void> rejectInvitation(@PathVariable String workspaceCode,
		@ResolveLoginMember LoginMember loginMember) {

		invitationService.rejectInvitation(loginMember.getId(), workspaceCode);
		return ApiResponse.ok("Invitation Rejected", null);
	}
}
