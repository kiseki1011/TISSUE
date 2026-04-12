package com.tissue.security.adapter.web;

import com.tissue.security.adapter.web.annotation.PublicApi;
import com.tissue.security.adapter.web.annotation.RequireEmail;
import com.tissue.security.adapter.web.request.PasswordResetRequest;
import com.tissue.security.adapter.web.request.ResetPasswordRequest;
import com.tissue.security.application.dto.response.PasswordResetRequestResponse;
import com.tissue.security.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.security.application.port.usecase.PasswordResetUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.servlet.ModelAndView;

@Tag(name = "Password Reset")
@RestController
@RequestMapping("/api/v1/members/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetUseCase passwordResetUseCase;

    @Operation(summary = "Reset password", description = """
                Set a new password using a verified email token.

                **Requirements:**
                - Requires a verified email token
                - Only available when `email-required` is enabled""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Password reset successfully"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request, invalid verified token, or `email-required` disabled",
                content = @Content)
    })
    @PublicApi
    @RequireEmail
    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        passwordResetUseCase.resetPassword(request.email(), request.verifiedToken(), request.newPassword());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Request password reset", description = """
                Send a password reset verification email.

                **Requirements:**
                - Only available when `email-required` is enabled""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset email sent"),
        @ApiResponse(responseCode = "400", description = "`email-required` disabled", content = @Content)
    })
    @PublicApi
    @RequireEmail
    @PostMapping("/reset-request")
    public ResponseEntity<PasswordResetRequestResponse> requestReset(@RequestBody @Valid PasswordResetRequest request) {
        String verificationId = passwordResetUseCase.requestPasswordReset(request.email());

        return ResponseEntity.ok(new PasswordResetRequestResponse(verificationId));
    }

    @Operation(summary = "Verify password reset email", description = """
                Verify email ownership via the link sent for password reset.\
                 Returns an HTML result page.

                **Requirements:**
                - Only available when `email-required` is enabled""")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "HTML verification result page",
                content = @Content(mediaType = "text/html")),
        @ApiResponse(responseCode = "400", description = "`email-required` disabled", content = @Content)
    })
    @PublicApi
    @RequireEmail
    @GetMapping("/verify")
    public ModelAndView verifyEmail(@RequestParam String token) {
        boolean verified = passwordResetUseCase.verifyEmailToken(token);
        String viewName = verified ? "verification-success" : "verification-failure";

        return new ModelAndView(viewName);
    }

    @Operation(summary = "Check reset verification status", description = """
                Poll the current status of a password reset verification request.

                **Requirements:**
                - Only available when `email-required` is enabled""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verification status retrieved"),
        @ApiResponse(responseCode = "400", description = "`email-required` disabled", content = @Content)
    })
    @PublicApi
    @RequireEmail
    @GetMapping("/status/{verificationId}")
    public ResponseEntity<VerificationStatus> getStatus(@PathVariable String verificationId) {
        VerificationStatus status = passwordResetUseCase.getVerificationStatus(verificationId);

        return ResponseEntity.ok(status);
    }
}
