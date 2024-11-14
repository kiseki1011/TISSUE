package com.uranus.taskmanager.api.invitation.presentation.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.global.interceptor.LoginRequired;
import com.uranus.taskmanager.api.global.resolver.ResolveLoginMember;
import com.uranus.taskmanager.api.security.authentication.presentation.dto.LoginMember;
import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.invitation.presentation.dto.response.InvitationAcceptResponse;
import com.uranus.taskmanager.api.invitation.service.InvitationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/invitations")
public class InvitationController {
	private final InvitationService invitationService;

	/*
	 * Todo
	 *  - 지금 초대 수락은 워크스페이스 코드 & 로그인 멤버 id를 통해 찾아서 진행
	 *  - 근데 성능은 초대의 id & 로그인 멤버 id를 통해서 찾는게 더 좋아 보임
	 *  - -> 내 의도는 직관적으로 워크스페이스에 대한 초대를 해당 워크스페이스의 코드를 통해 수락하자는 것이지만
	 *  - -> 생각보다 비효율적일 것 같다는 생각이 듬
	 */
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
