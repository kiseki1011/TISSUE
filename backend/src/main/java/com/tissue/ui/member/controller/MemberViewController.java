package com.tissue.ui.member.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.InvalidRequestException;
import com.tissue.api.member.application.service.command.MemberCommandService;
import com.tissue.api.member.application.service.command.MemberEmailVerificationService;
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

		SignupFormRequest request = SignupFormRequest.builder().build();

		model.addAttribute("signupFormRequest", request);

		return "member/signup";
	}

	// TODO: 공통 에러 페이지 추가 (4xx, 5xx, 6xx)
	// TODO: /members -> member 상세 정보 뷰 만들기
	@PostMapping("/signup")
	public String signup(
		@Valid @ModelAttribute("signupFormRequest") SignupFormRequest request,
		BindingResult bindingResult,
		Model model
	) {
		boolean isPasswordEmpty = request.password() == null || request.password().isBlank();
		model.addAttribute("isPasswordEmpty", isPasswordEmpty);

		if (bindingResult.hasErrors()) {
			boolean emailVerified = memberEmailVerificationService.isEmailVerified(request.email());
			model.addAttribute("emailVerified", emailVerified);

			return "member/signup";
		}

		try {
			memberCommandService.signup(request.toCommand());
			return "redirect:/members/signup/success";
			// return "redirect:/members";
		} catch (DuplicateResourceException | InvalidRequestException e) {
			model.addAttribute("globalError", e.getMessage());
			boolean emailVerified = memberEmailVerificationService.isEmailVerified(request.email());
			model.addAttribute("emailVerified", emailVerified);
			model.addAttribute("isPasswordEmpty", true);

			return "member/signup";
		}
	}

	@GetMapping("/signup/success")
	public String success(Model model) {
		model.addAttribute("message", "Signup was successful.");
		return "member/signup-success";
	}
}
