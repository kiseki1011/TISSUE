package com.tissue.member.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.common.dto.ApiResponse;
import com.tissue.member.application.service.MemberQueryService;
import com.tissue.member.application.dto.response.GetProfileResponse;
import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.resolver.CurrentMember;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberQueryController {

	private final MemberQueryService memberQueryService;

	@GetMapping
	public ApiResponse<GetProfileResponse> getProfile(
		@CurrentMember MemberUserDetails userDetails
	) {
		GetProfileResponse response = memberQueryService.getProfile(userDetails.getMemberId());
		return ApiResponse.ok("Found profile.", response);
	}

	// @GetMapping
	// public ApiResponse<GetWorkspacesResponse> getWorkspaces(
	// 	@CurrentMember MemberUserDetails userDetails,
	// 	Pageable pageable
	// ) {
	// 	GetWorkspacesResponse response = workspaceParticipationQueryService.getWorkspaces(
	// 		userDetails.getMemberId(),
	// 		pageable
	// 	);
	//
	// 	return ApiResponse.ok("Currently joined workspaces found.", response);
	// }
}
