package com.tissue.ui.member;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tissue.api.member.application.service.command.MemberCommandService;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberViewController {

	private final MemberCommandService memberCommandService;

	// TODO: SignupMemberRequest 팩토리 메서드 구현
	@GetMapping("/signup")
	public String signupForm(Model model) {

		SignupMemberRequest request = SignupMemberRequest.builder()
			.loginId("")
			.email("")
			.username("")
			.password("")
			.firstName("")
			.lastName("")
			.biography("")
			.build();

		model.addAttribute("member", request);

		return "member/signup";
	}

	// TODO: 공통 에러 페이지 추가 (4xx, 5xx, 6xx)
	// TODO: /members -> member 상세 정보 뷰 만들기
	@PostMapping("/signup")
	public String signup(@ModelAttribute SignupMemberRequest request, Model model) {
		memberCommandService.signup(request);

		model.addAttribute("message", "Signup successful");
		return "redirect:/members/signup/success";
		// return "redirect:/members";
	}

	@GetMapping("/signup/success")
	public String success(Model model) {
		model.addAttribute("message", "Signup was successful.");
		return "member/signup-success";
	}
}
