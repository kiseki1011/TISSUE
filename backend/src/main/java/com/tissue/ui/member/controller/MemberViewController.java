package com.tissue.ui.member.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.InvalidRequestException;
import com.tissue.api.member.application.service.command.MemberCommandService;
import com.tissue.api.member.application.service.command.MemberEmailVerificationService;
import com.tissue.api.member.domain.model.enums.JobType;
import com.tissue.api.member.presentation.dto.response.command.MemberResponse;
import com.tissue.ui.member.dto.request.SignupFormRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberViewController {

	private final MemberCommandService memberCommandService;
	private final MemberEmailVerificationService memberEmailVerificationService;

	// TODO: SignupMemberRequest 팩토리 메서드 구현
	@GetMapping("/signup")
	public String signupForm(Model model) {

		SignupFormRequest request = SignupFormRequest.builder()
			.loginId("")
			.firstName("")
			.lastName("")
			.password("")
			.email("")
			.jobType(JobType.ETC)
			.build();

		model.addAttribute("signupFormRequest", request);

		// 서버 사이드 유효성 검사 에러가 있을 경우를 위한 준비
		model.addAttribute("emailVerified", false);
		model.addAttribute("validationErrors", new HashMap<>());

		return "member/signup_terminal";
	}

	@PostMapping("/signup")
	public String signup(
		@Valid @ModelAttribute("signupFormRequest") SignupFormRequest request,
		BindingResult bindingResult,
		Model model,
		RedirectAttributes redirectAttributes
	) {

		// 1. 기본 유효성 검사 실패 시
		if (bindingResult.hasErrors()) {
			return handleSignupErrors(request, bindingResult, model);
		}

		try {
			// 2. 비즈니스 로직 실행
			MemberResponse memberResponse = memberCommandService.signup(request.toCommand());

			// 3. 성공 시 리다이렉트
			redirectAttributes.addFlashAttribute("memberResponse", memberResponse);
			return "redirect:/members/signup/success";

		} catch (DuplicateResourceException | InvalidRequestException e) {
			// 4. 비즈니스 예외 처리
			return handleBusinessException(request, e, model);
		}
	}

	/**
	 * 유효성 검사 에러 처리
	 */
	private String handleSignupErrors(SignupFormRequest request, BindingResult bindingResult, Model model) {
		// 유효성 검사 에러를 프론트엔드에서 사용할 수 있는 형태로 변환
		Map<String, String> errors = new HashMap<>();
		bindingResult.getFieldErrors().forEach(error -> {
			errors.put(error.getField(), error.getDefaultMessage());
		});

		model.addAttribute("validationErrors", errors);
		model.addAttribute("signupFormRequest", request);

		// 이메일 인증 상태 확인
		if (request.email() != null && !request.email().isBlank()) {
			boolean emailVerified = memberEmailVerificationService.isEmailVerified(request.email());
			model.addAttribute("emailVerified", emailVerified);
		}

		return "member/signup_terminal";
	}

	/**
	 * 비즈니스 예외 처리
	 */
	private String handleBusinessException(SignupFormRequest request, Exception e, Model model) {
		model.addAttribute("globalError", e.getMessage());
		model.addAttribute("signupFormRequest", request);

		// 이메일 인증 상태 재확인
		if (request.email() != null) {
			boolean emailVerified = memberEmailVerificationService.isEmailVerified(request.email());
			model.addAttribute("emailVerified", emailVerified);
		}

		return "member/signup_terminal";
	}

	@GetMapping("/signup/success")
	public String success(Model model) {
		model.addAttribute("message", "Signup was successful.");
		return "member/signup-success";
	}
}
