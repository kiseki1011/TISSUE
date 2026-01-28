package com.tissue.member.adapter.in.web;

import com.tissue.member.adapter.in.web.request.EmailVerificationRequest;
import com.tissue.member.application.port.out.EmailVerificationRepository.VerificationStatus;
import com.tissue.member.application.service.MemberEmailVerificationService;
import com.tissue.member.infrastructure.config.EmailVerificationProperties;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/api/v1/members/verification")
@RequiredArgsConstructor
public class MemberEmailVerificationController {

    private final MemberEmailVerificationService memberEmailVerificationService;
    private final EmailVerificationProperties properties;

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestVerification(
            @RequestBody @Valid EmailVerificationRequest request) {
        String verificationId = memberEmailVerificationService.sendVerificationEmail(request.email());
        return ResponseEntity.ok(Map.of("verificationId", verificationId));
    }

    @GetMapping("/verify")
    public ModelAndView verifyEmail(@RequestParam String token) {
        boolean verified = memberEmailVerificationService.verifyEmail(token);
        String viewName = verified ? "verification-success" : "verification-failure";
        return new ModelAndView(viewName);
    }

    @GetMapping("/{verificationId}/status")
    public ResponseEntity<VerificationStatus> checkVerification(@PathVariable String verificationId) {
        VerificationStatus status = memberEmailVerificationService.getVerificationStatus(verificationId);
        return ResponseEntity.ok(status);
    }
}
