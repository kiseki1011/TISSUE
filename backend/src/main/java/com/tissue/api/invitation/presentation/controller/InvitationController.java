package com.tissue.api.invitation.presentation.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.common.dto.PageResponse;
import com.tissue.api.invitation.presentation.dto.InvitationSearchCondition;
import com.tissue.api.invitation.presentation.dto.response.AcceptInvitationResponse;
import com.tissue.api.invitation.presentation.dto.response.InvitationResponse;
import com.tissue.api.invitation.presentation.dto.response.RejectInvitationResponse;
import com.tissue.api.invitation.service.command.InvitationCommandService;
import com.tissue.api.invitation.service.query.InvitationQueryService;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/invitations")
public class InvitationController {

	private final InvitationCommandService invitationCommandService;
	private final InvitationQueryService invitationQueryService;

	@LoginRequired
	@GetMapping
	public ApiResponse<PageResponse<InvitationResponse>> getMyInvitations(
		@ResolveLoginMember Long loginMemberId,
		InvitationSearchCondition searchCondition,
		@PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
	) {
		Page<InvitationResponse> page = invitationQueryService.getInvitations(
			loginMemberId,
			searchCondition,
			pageable
		);
		return ApiResponse.ok("Found invitations", PageResponse.of(page));
	}

	@LoginRequired
	@PostMapping("/{invitationId}/accept")
	public ApiResponse<AcceptInvitationResponse> acceptInvitation(
		@PathVariable Long invitationId,
		@ResolveLoginMember Long loginMemberId
	) {
		AcceptInvitationResponse response = invitationCommandService.acceptInvitation(
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
		RejectInvitationResponse response = invitationCommandService.rejectInvitation(
			loginMemberId,
			invitationId
		);
		return ApiResponse.ok("Invitation Rejected.", response);
	}

	@LoginRequired
	@DeleteMapping
	public ApiResponse<Void> deleteInvitations(
		@ResolveLoginMember Long loginMemberId
	) {
		invitationCommandService.deleteInvitations(loginMemberId);
		return ApiResponse.okWithNoContent("Invitation history deleted.");
	}
}
