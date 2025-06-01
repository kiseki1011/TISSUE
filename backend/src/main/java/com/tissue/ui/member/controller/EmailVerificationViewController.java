package com.tissue.ui.member.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("members/email-verification")
public class EmailVerificationViewController {

	@GetMapping("/success")
	public String success(Model model) {
		model.addAttribute("message", "Email verification completed successfully.");
		return "verification/verify-success";
	}

	@GetMapping("/fail")
	public String fail(Model model) {
		model.addAttribute("message", "Verification failed or token expired.");
		return "verification/verify-fail";
	}
}
