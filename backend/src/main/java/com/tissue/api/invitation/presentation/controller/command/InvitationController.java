package com.tissue.api.invitation.presentation.controller.command;

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
import com.tissue.api.invitation.application.service.command.InvitationCommandService;
import com.tissue.api.invitation.application.service.query.InvitationQueryService;
import com.tissue.api.invitation.presentation.controller.query.InvitationSearchCondition;
import com.tissue.api.invitation.presentation.dto.response.InvitationDetail;
import com.tissue.api.invitation.presentation.dto.response.InvitationResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/invitations")
public class InvitationController {

	private final InvitationCommandService invitationCommandService;
	private final InvitationQueryService invitationQueryService;

	@GetMapping
	public ApiResponse<PageResponse<InvitationDetail>> getMyInvitations(
		@CurrentMember MemberUserDetails userDetails,
		InvitationSearchCondition searchCondition,
		@PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
	) {
		Page<InvitationDetail> page = invitationQueryService.getInvitations(
			userDetails.getMemberId(),
			searchCondition,
			pageable
		);
		return ApiResponse.ok("Found invitations", PageResponse.of(page));
	}

	@PostMapping("/{invitationId}/accept")
	public ApiResponse<InvitationResponse> acceptInvitation(
		@PathVariable Long invitationId,
		@CurrentMember MemberUserDetails userDetails
	) {
		InvitationResponse response = invitationCommandService.acceptInvitation(
			userDetails.getMemberId(),
			invitationId
		);
		return ApiResponse.ok("Invitation Accepted.", response);
	}

	@PostMapping("/{invitationId}/reject")
	public ApiResponse<InvitationResponse> rejectInvitation(
		@PathVariable Long invitationId,
		@CurrentMember MemberUserDetails userDetails
	) {
		InvitationResponse response = invitationCommandService.rejectInvitation(
			userDetails.getMemberId(),
			invitationId
		);
		return ApiResponse.ok("Invitation Rejected.", response);
	}

	@DeleteMapping
	public ApiResponse<Void> deleteInvitations(
		@CurrentMember MemberUserDetails userDetails
	) {
		invitationCommandService.deleteInvitations(userDetails.getMemberId());
		return ApiResponse.okWithNoContent("Invitation history deleted.");
	}
}
