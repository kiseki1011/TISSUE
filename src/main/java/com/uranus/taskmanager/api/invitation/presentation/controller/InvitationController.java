package com.uranus.taskmanager.api.invitation.presentation.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.invitation.presentation.dto.response.AcceptInvitationResponse;
import com.uranus.taskmanager.api.invitation.presentation.dto.response.RejectInvitationResponse;
import com.uranus.taskmanager.api.invitation.service.InvitationService;
import com.uranus.taskmanager.api.security.authentication.interceptor.LoginRequired;
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
	@PostMapping("/{invitationId}/accept")
	public ApiResponse<AcceptInvitationResponse> acceptInvitation(
		@PathVariable Long invitationId,
		@ResolveLoginMember Long loginMemberId
	) {

		AcceptInvitationResponse response = invitationService.acceptInvitation(
			loginMemberId,
			invitationId
		);
		return ApiResponse.ok("Invitation Accepted.", response);
	}

	@LoginRequired
	@PostMapping("/{invitationId}/reject")
	public ApiResponse<RejectInvitationResponse> rejectInvitation(
		@PathVariable Long invitationId,
		@ResolveLoginMember Long loginMemberId
	) {

		RejectInvitationResponse response = invitationService.rejectInvitation(
			loginMemberId,
			invitationId
		);
		return ApiResponse.ok("Invitation Rejected.", response);
	}
}
