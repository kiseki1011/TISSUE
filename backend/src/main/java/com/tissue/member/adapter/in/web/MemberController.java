package com.tissue.member.adapter.in.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tissue.member.adapter.in.web.dto.request.SignupMemberRequest;
import com.tissue.member.adapter.in.web.dto.request.UpdateMemberEmailRequest;
import com.tissue.member.adapter.in.web.dto.request.UpdateMemberNameRequest;
import com.tissue.member.adapter.in.web.dto.request.UpdateMemberPasswordRequest;
import com.tissue.member.adapter.in.web.dto.request.UpdateMemberUsernameRequest;
import com.tissue.member.adapter.in.web.dto.request.WithdrawMemberRequest;
import com.tissue.member.application.dto.response.MemberSignupResponse;
import com.tissue.member.application.port.in.MemberCommandUseCase;
import com.tissue.member.application.service.validator.MemberValidator;
import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.exception.AuthenticationExceptions;
import com.tissue.security.authentication.resolver.CurrentMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// TODO(later): consdier OAuth
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberCommandUseCase memberCommandUseCase;
	private final MemberValidator memberValidator;

	@PostMapping
	public ResponseEntity<MemberSignupResponse> signup(
		@Valid @RequestBody SignupMemberRequest request
	) {
		var command = request.toCommand();
		MemberSignupResponse response = memberCommandUseCase.signup(command);

		URI location = ServletUriComponentsBuilder
			.fromCurrentRequest()
			.path("/{memberId}")
			.buildAndExpand(response.memberId())
			.toUri();

		return ResponseEntity.created(location)
			.body(response);
	}

	@PatchMapping("/name")
	public ResponseEntity<Void> updateMemberName(
		@RequestBody @Valid UpdateMemberNameRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		memberCommandUseCase.updateName(request.newName(), userDetails.getMemberId());

		return ResponseEntity.noContent().build();
	}

	// TODO(later): consider 2-factor
	@PatchMapping("/email")
	public ResponseEntity<Void> updateMemberEmail(
		@RequestBody @Valid UpdateMemberEmailRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		validatePermissionElevated(userDetails);

		memberCommandUseCase.updateEmail(request.newEmail(), userDetails.getMemberId());

		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/username")
	public ResponseEntity<MemberSignupResponse> updateMemberUsername(
		@RequestBody @Valid UpdateMemberUsernameRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		validatePermissionElevated(userDetails);

		memberCommandUseCase.updateUsername(request.newUsername(), userDetails.getMemberId());

		return ResponseEntity.noContent().build();
	}

	// TODO(later): consider 2-factor
	@PatchMapping("/password")
	public ResponseEntity<MemberSignupResponse> updateMemberPassword(
		@RequestBody @Valid UpdateMemberPasswordRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		validatePermissionElevated(userDetails);

		memberCommandUseCase.updatePassword(
			request.originalPassword(),
			request.newPassword(),
			userDetails.getMemberId()
		);

		return ResponseEntity.noContent().build();
	}

	@DeleteMapping
	public ResponseEntity<Void> withdrawMember(
		@RequestBody WithdrawMemberRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		validatePermissionElevated(userDetails);

		memberCommandUseCase.withdraw(request.password(), userDetails.getMemberId());

		return ResponseEntity.noContent().build();
	}

	// TODO(later): resetPassword
	//  1. send email with a short-life(15~30 min) token
	//  2. email should have a password reset link
	//  3. change password through that link -> expire token

	/**
	 * Check email uniqueness
	 */
	@GetMapping("/checkEmail")
	public ResponseEntity<Void> checkEmailAvailability(@RequestParam String email) {
		// TODO: should i delegate this to a service and not call member validator directly?
		memberValidator.ensureUniqueEmail(email);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Check username uniqueness
	 */
	@GetMapping("/checkUsername")
	public ResponseEntity<Void> checkUsernameAvailability(@RequestParam String username) {
		memberValidator.ensureUniqueUsername(username);
		return ResponseEntity.noContent().build();
	}

	private void validatePermissionElevated(MemberUserDetails userDetails) {
		boolean notElevated = !userDetails.isElevated();
		if (notElevated) {
			throw AuthenticationExceptions.elevatedPermissionRequired();
		}
	}
}
