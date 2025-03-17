package com.tissue.api.member.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.common.enums.PermissionType;
import com.tissue.api.member.presentation.dto.request.PermissionRequest;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberEmailRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberInfoRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberPasswordRequest;
import com.tissue.api.member.presentation.dto.request.WithdrawMemberRequest;
import com.tissue.api.member.presentation.dto.response.GetProfileResponse;
import com.tissue.api.member.presentation.dto.response.SignupMemberResponse;
import com.tissue.api.member.presentation.dto.response.UpdateMemberEmailResponse;
import com.tissue.api.member.presentation.dto.response.UpdateMemberInfoResponse;
import com.tissue.api.member.service.command.MemberCommandService;
import com.tissue.api.member.service.query.MemberQueryService;
import com.tissue.api.member.validator.MemberValidator;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.security.session.SessionManager;
import com.tissue.api.security.session.SessionValidator;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {
	/**
	 * Todo
	 *  - 비밀번호 찾기 (세션 불필요)
	 * 	  - 가입한 이메일, 로그인 ID를 통한 비밀번호 찾기
	 * 	  - 기입한 로그인 ID, 이메일이 일치하면 이메일로 임시 비밀번호 보내기
	 * 	  - 또는 비밀번호 재설정 링크 보내기
	 * 	- 회원 가입 시, 이메일 확인 로직 필요(이메일로 확인 이메일 보내기)
	 *  - 이메일 업데이트 시, 이메일로 확인(검증) 이메일 보내기
	 */
	private final MemberCommandService memberCommandService;
	private final MemberQueryService memberQueryService;
	private final MemberValidator memberValidator;
	private final SessionManager sessionManager;
	private final SessionValidator sessionValidator;

	@GetMapping
	public ApiResponse<GetProfileResponse> getProfile(
		@ResolveLoginMember Long loginMemberId
	) {
		GetProfileResponse response = memberQueryService.getProfile(loginMemberId);
		return ApiResponse.ok("Found profile.", response);
	}

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<SignupMemberResponse> signup(
		@Valid @RequestBody SignupMemberRequest request
	) {
		SignupMemberResponse response = memberCommandService.signup(request);
		return ApiResponse.created("Signup successful.", response);
	}

	@LoginRequired
	@PatchMapping
	public ApiResponse<UpdateMemberInfoResponse> updateMemberInfo(
		@RequestBody @Valid UpdateMemberInfoRequest request,
		@ResolveLoginMember Long loginMemberId,
		HttpSession session
	) {
		sessionValidator.validatePermissionInSession(session, PermissionType.MEMBER_UPDATE);
		UpdateMemberInfoResponse response = memberCommandService.updateInfo(request, loginMemberId);

		return ApiResponse.ok("Member info updated.", response);
	}

	@LoginRequired
	@PatchMapping("/email")
	public ApiResponse<UpdateMemberEmailResponse> updateMemberEmail(
		@RequestBody @Valid UpdateMemberEmailRequest request,
		@ResolveLoginMember Long loginMemberId,
		HttpSession session
	) {
		memberValidator.validateMemberPassword(request.password(), loginMemberId);
		UpdateMemberEmailResponse response = memberCommandService.updateEmail(request, loginMemberId);
		sessionManager.updateSessionEmail(session, request.newEmail());

		return ApiResponse.ok("Member email updated.", response);
	}

	@LoginRequired
	@PatchMapping("/password")
	public ApiResponse<Void> updateMemberPassword(
		@RequestBody @Valid UpdateMemberPasswordRequest request,
		@ResolveLoginMember Long loginMemberId,
		HttpSession session
	) {
		memberValidator.validateMemberPassword(request.originalPassword(), loginMemberId);
		memberCommandService.updatePassword(request, loginMemberId);

		return ApiResponse.okWithNoContent("Member password updated.");
	}

	/**
	 * Todo
	 *  - hard delete X
	 *  - INACTIVE 또는 WITHDRAW_REQUESTED 상태로 변경(MembershipStatus 만들기)
	 *  - 추후에 스케쥴을 사용해서 배치로 삭제
	 *  - INACTIVE 상태인 멤버는 로그인 불가능하도록 막기(기존 로그인 세션도 전부 제거)
	 */
	@LoginRequired
	@DeleteMapping
	public ApiResponse<Void> withdrawMember(
		@RequestBody WithdrawMemberRequest request,
		@ResolveLoginMember Long loginMemberId,
		HttpSession session
	) {
		memberValidator.validateMemberPassword(request.password(), loginMemberId);
		memberCommandService.withdraw(loginMemberId);
		session.invalidate();

		return ApiResponse.okWithNoContent("Member withdrawal successful.");
	}

	@LoginRequired
	@PostMapping("/permissions/update")
	public ApiResponse<Void> getMemberUpdatePermission(
		@RequestBody @Valid PermissionRequest request,
		@ResolveLoginMember Long loginMemberId,
		HttpSession session
	) {
		memberValidator.validateMemberPassword(request.password(), loginMemberId);
		sessionManager.setTemporaryPermission(session, PermissionType.MEMBER_UPDATE);

		return ApiResponse.okWithNoContent("Update permission granted.");
	}
}
