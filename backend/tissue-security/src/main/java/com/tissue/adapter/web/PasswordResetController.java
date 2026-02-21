package com.tissue.adapter.web;

import com.tissue.adapter.web.request.PasswordResetRequest;
import com.tissue.adapter.web.request.ResetPasswordRequest;
import com.tissue.adapter.web.request.VerifyResetCodeRequest;
import com.tissue.application.dto.response.VerifyCodeResponse;
import com.tissue.application.port.usecase.PasswordResetUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetUseCase passwordResetUseCase;

    @PostMapping("/reset-request")
    public ResponseEntity<Void> requestReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetUseCase.requestPasswordReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-code")
    public ResponseEntity<VerifyCodeResponse> verifyCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        String resetToken = passwordResetUseCase.verifyResetCode(request.email(), request.code());
        return ResponseEntity.ok(new VerifyCodeResponse(resetToken));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetUseCase.resetPassword(request.resetToken(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
