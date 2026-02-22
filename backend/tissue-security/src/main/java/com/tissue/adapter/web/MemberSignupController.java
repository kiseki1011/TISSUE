package com.tissue.adapter.web;

import com.tissue.adapter.web.request.EmailVerificationRequest;
import com.tissue.adapter.web.request.SignupMemberRequest;
import com.tissue.adapter.web.request.SignupOAuthMemberRequest;
import com.tissue.application.dto.response.MemberSignupResponse;
import com.tissue.application.dto.response.OAuthSignupResponse;
import com.tissue.application.dto.response.SignupVerificationResponse;
import com.tissue.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.application.port.usecase.MemberSignupUseCase;
import com.tissue.application.service.MemberEmailVerificationService;
import jakarta.validation.Valid;
import java.net.URI;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/members/signup")
@RequiredArgsConstructor
public class MemberSignupController {

    private final MemberSignupUseCase memberSignupUseCase;
    private final MemberEmailVerificationService memberEmailVerificationService;

    @PostMapping("/email")
    public ResponseEntity<MemberSignupResponse> signup(@Valid @RequestBody SignupMemberRequest request) {
        var command = request.toCommand();
        MemberSignupResponse response = memberSignupUseCase.signupWithEmail(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{memberId}")
                .buildAndExpand(response.memberId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/oauth")
    public ResponseEntity<OAuthSignupResponse> signupOAuth(@Valid @RequestBody SignupOAuthMemberRequest request) {
        OAuthSignupResponse response = memberSignupUseCase.signupWithOAuth(request.toCommand());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request-verification")
    public ResponseEntity<SignupVerificationResponse> requestVerification(
            @RequestBody @Valid EmailVerificationRequest request) {

        String verificationId = memberEmailVerificationService.sendSignupVerificationEmail(request.email());
        return ResponseEntity.ok(new SignupVerificationResponse(verificationId));
    }

    @GetMapping("/verify")
    public ModelAndView verifyEmail(@RequestParam String token) {
        boolean verified = memberEmailVerificationService.verifyEmail(token);
        String viewName = verified ? "verification-success" : "verification-failure";
        return new ModelAndView(viewName);
    }

    @GetMapping("/status/{verificationId}")
    public ResponseEntity<VerificationStatus> checkVerification(@PathVariable String verificationId) {
        VerificationStatus status = memberEmailVerificationService.getVerificationStatus(verificationId);
        return ResponseEntity.ok(status);
    }
}
