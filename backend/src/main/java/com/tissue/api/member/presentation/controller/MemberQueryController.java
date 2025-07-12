package com.tissue.api.member.presentation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.member.application.service.query.MemberQueryService;
import com.tissue.api.member.presentation.dto.response.query.GetProfileResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;

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
}
