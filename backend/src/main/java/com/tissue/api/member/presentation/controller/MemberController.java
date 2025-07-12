package com.tissue.api.member.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.member.application.service.command.MemberCommandService;
import com.tissue.api.member.domain.service.MemberValidator;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberEmailRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberPasswordRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberProfileRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberUsernameRequest;
import com.tissue.api.member.presentation.dto.request.WithdrawMemberRequest;
import com.tissue.api.member.presentation.dto.response.command.MemberResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;

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
	private final MemberValidator memberValidator;

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<MemberResponse> signup(
		@Valid @RequestBody SignupMemberRequest request
	) {
		MemberResponse response = memberCommandService.signup(request.toCommand());
		return ApiResponse.created("Signup successful.", response);
	}

	@PatchMapping
	public ApiResponse<MemberResponse> updateMemberInfo(
		@RequestBody @Valid UpdateMemberProfileRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		MemberResponse response = memberCommandService.updateInfo(request, userDetails.getMemberId());

		return ApiResponse.ok("Member info updated.", response);
	}

	@PatchMapping("/email")
	public ApiResponse<MemberResponse> updateMemberEmail(
		@RequestBody @Valid UpdateMemberEmailRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		boolean notElevated = !userDetails.isElevated();
		if (notElevated) {
			throw new ForbiddenOperationException("Elevated permission required.");
		}

		MemberResponse response = memberCommandService.updateEmail(request, userDetails.getMemberId());

		return ApiResponse.ok("Member email updated.", response);
	}

	@PatchMapping("/username")
	public ApiResponse<MemberResponse> updateMemberUsername(
		@RequestBody @Valid UpdateMemberUsernameRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		boolean notElevated = !userDetails.isElevated();
		if (notElevated) {
			throw new ForbiddenOperationException("Elevated permission required.");
		}

		MemberResponse response = memberCommandService.updateUsername(request, userDetails.getMemberId());

		return ApiResponse.ok("Member username updated.", response);
	}

	@PatchMapping("/password")
	public ApiResponse<MemberResponse> updateMemberPassword(
		@RequestBody @Valid UpdateMemberPasswordRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		boolean notElevated = !userDetails.isElevated();
		if (notElevated) {
			throw new ForbiddenOperationException("Elevated permission required.");
		}

		MemberResponse response = memberCommandService.updatePassword(request, userDetails.getMemberId());

		return ApiResponse.ok("Member password updated.", response);
	}

	/**
	 * Todo
	 *  - soft delete으로 변경
	 *  - INACTIVE 또는 WITHDRAW_REQUESTED 상태로 변경(MembershipStatus 만들기)
	 *  - 추후에 스케쥴을 사용해서 배치로 삭제
	 *  - INACTIVE 상태인 멤버는 로그인 불가능하도록 막기(기존 로그인 세션도 전부 제거)
	 *  - soft delete으로 변경 시 MemberResponse 사용
	 */

	@DeleteMapping
	public ApiResponse<Void> withdrawMember(
		@RequestBody WithdrawMemberRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		boolean notElevated = !userDetails.isElevated();
		if (notElevated) {
			throw new ForbiddenOperationException("Elevated permission required.");
		}

		memberCommandService.withdraw(request, userDetails.getMemberId());

		return ApiResponse.okWithNoContent("Member withdrawal successful.");
	}

	/**
	 * Login ID 중복 검사
	 */
	@GetMapping("/check-loginid")
	public ApiResponse<Void> checkLoginIdAvailability(@RequestParam String loginId) {
		memberValidator.validateLoginIdIsUnique(loginId);
		return ApiResponse.okWithNoContent("Login ID is available.");
	}

	/**
	 * 이메일 중복 검사
	 */
	@GetMapping("/check-email")
	public ApiResponse<Void> checkEmailAvailability(@RequestParam String email) {
		memberValidator.validateEmailIsUnique(email);
		return ApiResponse.okWithNoContent("Email is available.");
	}

	/**
	 * 사용자명 중복 검사
	 */
	@GetMapping("/check-username")
	public ApiResponse<Void> checkUsernameAvailability(@RequestParam String username) {
		memberValidator.validateUsernameIsUnique(username);
		return ApiResponse.okWithNoContent("Username is available.");
	}
}
