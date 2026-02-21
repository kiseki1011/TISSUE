package com.tissue.adapter.web;

import com.tissue.adapter.web.request.PasswordResetRequest;
import com.tissue.adapter.web.request.ResetPasswordRequest;
import com.tissue.application.dto.response.PasswordResetRequestResponse;
import com.tissue.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.application.port.usecase.PasswordResetUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetUseCase passwordResetUseCase;

    @PostMapping("/reset-request")
    public ResponseEntity<PasswordResetRequestResponse> requestReset(@Valid @RequestBody PasswordResetRequest request) {
        String verificationId = passwordResetUseCase.requestPasswordReset(request.email());
        return ResponseEntity.ok(new PasswordResetRequestResponse(verificationId));
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token) {
        passwordResetUseCase.verifyEmailToken(token);
        return "verification-success";
    }

    @GetMapping("/status/{verificationId}")
    public ResponseEntity<VerificationStatus> getStatus(@PathVariable String verificationId) {
        VerificationStatus status = passwordResetUseCase.getVerificationStatus(verificationId);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetUseCase.resetPassword(request.resetToken(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
