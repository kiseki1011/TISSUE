package com.tissue.security.adapter.web;

import com.tissue.security.adapter.web.annotation.PublicApi;
import com.tissue.security.adapter.web.annotation.RequireEmail;
import com.tissue.security.adapter.web.request.EmailVerificationRequest;
import com.tissue.security.adapter.web.request.SignupMemberRequest;
import com.tissue.security.adapter.web.request.SignupOAuthMemberRequest;
import com.tissue.security.application.dto.response.MemberSignupResponse;
import com.tissue.security.application.dto.response.OAuthSignupResponse;
import com.tissue.security.application.dto.response.SignupVerificationResponse;
import com.tissue.security.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.security.application.port.usecase.MemberSignupUseCase;
import com.tissue.security.application.service.MemberEmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Member Signup")
@RestController
@RequestMapping("/api/v1/members/signup")
@RequiredArgsConstructor
public class MemberSignupController {

    private final MemberSignupUseCase memberSignupUseCase;
    private final MemberEmailVerificationService memberEmailVerificationService;

    @Operation(summary = "Sign up", description = """
                Register a new member.
                 The identifier is either `email` or `username` depending on the server's \
                 `email-required` setting.

                **Requirements:**
                - `email` or `username` must be unique
                - When `email-required` is enabled, a verified email token is also required""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Member created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "409", description = "Email or username already exists", content = @Content)
    })
    @PublicApi
    @PostMapping
    public ResponseEntity<MemberSignupResponse> signup(@Valid @RequestBody SignupMemberRequest request) {
        var command = request.toCommand();
        MemberSignupResponse response = memberSignupUseCase.signup(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{memberId}")
                .buildAndExpand(response.memberId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Sign up with OAuth", description = """
                Register a new member using an OAuth provider.

                **Requirements:**
                - Requires a register token obtained from the OAuth callback
                - `username` must be unique""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OAuth signup successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid or expired register token", content = @Content),
        @ApiResponse(responseCode = "409", description = "Username already exists", content = @Content)
    })
    @PublicApi
    @PostMapping("/oauth")
    public ResponseEntity<OAuthSignupResponse> signupOAuth(@Valid @RequestBody SignupOAuthMemberRequest request) {
        OAuthSignupResponse response = memberSignupUseCase.signupWithOAuth(request.toCommand());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Request email verification", description = """
                Send a verification email to the given address.

                **Requirements:**
                - Only available when `email-required` is enabled
                - `email` must not already be in use""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verification email sent"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content)
    })
    @PublicApi
    @RequireEmail
    @PostMapping("/request-verification")
    public ResponseEntity<SignupVerificationResponse> requestVerification(
            @RequestBody @Valid EmailVerificationRequest request) {
        String verificationId = memberEmailVerificationService.sendSignupVerificationEmail(request.email());

        return ResponseEntity.ok(new SignupVerificationResponse(verificationId));
    }

    @Operation(summary = "Verify email", description = """
                Verify email ownership via the link sent to the requester's email.\
                 Returns an HTML result page.

                **Requirements:**
                - Only available when `email-required` is enabled""")
    @ApiResponse(responseCode = "200", description = "HTML verification result page")
    @PublicApi
    @RequireEmail
    @GetMapping("/verify")
    public ModelAndView verifyEmail(@RequestParam String token) {
        boolean verified = memberEmailVerificationService.verifyEmail(token);
        String viewName = verified ? "verification-success" : "verification-failure";

        return new ModelAndView(viewName);
    }

    @Operation(summary = "Check verification status", description = """
                Poll the current status of an email verification request.

                **Requirements:**
                - Only available when `email-required` is enabled""")
    @ApiResponse(responseCode = "200", description = "Verification status retrieved")
    @PublicApi
    @RequireEmail
    @GetMapping("/status/{verificationId}")
    public ResponseEntity<VerificationStatus> checkVerification(@PathVariable String verificationId) {
        VerificationStatus status = memberEmailVerificationService.getVerificationStatus(verificationId);

        return ResponseEntity.ok(status);
    }
}
