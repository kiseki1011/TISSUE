package com.tissue.ui.member.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tissue.api.member.application.service.command.MemberCommandService;
import com.tissue.ui.member.dto.request.SignupFormRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberViewController {

	private final MemberCommandService memberCommandService;

	// TODO: SignupMemberRequest 팩토리 메서드 구현
	@GetMapping("/signup")
	public String signupForm(Model model) {

		SignupFormRequest request = SignupFormRequest.builder().build();

		model.addAttribute("signupMemberRequest", request);

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
		if (bindingResult.hasErrors()) {
			return "member/signup";
		}

		memberCommandService.signup(request.toCommand());

		return "redirect:/members/signup/success";
		// return "redirect:/members";
	}

	@GetMapping("/signup/success")
	public String success(Model model) {
		model.addAttribute("message", "Signup was successful.");
		return "member/signup-success";
	}
}
