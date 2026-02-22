package com.tissue.adapter.web;

import com.tissue.adapter.web.request.EmailVerificationRequest;
import com.tissue.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.application.service.MemberEmailVerificationService;
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
public class SignupEmailVerificationController {

    private final MemberEmailVerificationService memberEmailVerificationService;

    // TODO: Map<String, String> 대신 VerificationRequestResponse 만들어서 사용
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestVerification(
            @RequestBody @Valid EmailVerificationRequest request) {

        String verificationId = memberEmailVerificationService.sendSignupVerificationEmail(request.email());
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
