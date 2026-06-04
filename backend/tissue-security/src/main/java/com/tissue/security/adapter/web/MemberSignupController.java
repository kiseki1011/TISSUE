package com.tissue.security.adapter.web;

import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.global.openapi.AuthenticationErrors;
import com.tissue.global.openapi.CommonErrors;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.security.adapter.web.annotation.PublicApi;
import com.tissue.security.adapter.web.annotation.RequireEmail;
import com.tissue.security.adapter.web.request.EmailVerificationRequest;
import com.tissue.security.adapter.web.request.SignupMemberRequest;
import com.tissue.security.application.dto.response.MemberSignupResponse;
import com.tissue.security.application.dto.response.SignupVerificationResponse;
import com.tissue.security.application.port.repository.EmailVerificationRepository.VerificationStatus;
import com.tissue.security.application.port.usecase.MemberSignupUseCase;
import com.tissue.security.application.service.MemberEmailVerificationService;
import com.tissue.security.domain.exception.AuthenticationErrorCode;
import com.tissue.shared.auth.LocalAuthOnly;
import com.tissue.shared.exception.CommonErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@Tag(name = "Member Signup")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@LocalAuthOnly
public class MemberSignupController {

    private final MemberSignupUseCase memberSignupUseCase;
    private final MemberEmailVerificationService memberEmailVerificationService;

    @Operation(operationId = "signup", summary = "Sign up", description = """
                Register a new member.
                 The identifier is either `email` or `username` depending on the server's \
                 `email-required` setting.

                **Requirements:**
                - `email` or `username` must be unique
                - When `email-required` is enabled, a verified email token is also required
                - **Unavailable in OIDC mode**""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Member created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @AuthenticationErrors({
        AuthenticationErrorCode.EMAIL_SIGNUP_DISABLED,
        AuthenticationErrorCode.EMAIL_NOT_VERIFIED,
        AuthenticationErrorCode.MEMBER_SIGNUP_CONFLICT,
    })
    @MemberErrors({
        MemberErrorCode.DUPLICATE_USERNAME,
        MemberErrorCode.DUPLICATE_EMAIL,
    })
    @PublicApi
    @PostMapping("/signup")
    public ResponseEntity<MemberSignupResponse> signup(@RequestBody @Valid SignupMemberRequest request) {
        var command = request.toCommand();
        MemberSignupResponse response = memberSignupUseCase.signup(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "requestSignupVerification", summary = "Request email verification", description = """
                Send a verification email to the given address.

                **Requirements:**
                - Only available when `email-required` is enabled
                - **Unavailable in OIDC mode**""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verification email sent"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "429", description = "Too many requests", content = @Content)
    })
    @AuthenticationErrors({AuthenticationErrorCode.EMAIL_FEATURE_DISABLED})
    @CommonErrors({CommonErrorCode.RATE_LIMITED})
    @PublicApi
    @RequireEmail
    @PostMapping("/signup:requestVerification")
    public ResponseEntity<SignupVerificationResponse> requestSignupVerification(
            @RequestBody @Valid EmailVerificationRequest request) {
        String verificationId = memberEmailVerificationService.sendSignupVerificationEmail(request.email());

        return ResponseEntity.ok(new SignupVerificationResponse(verificationId));
    }

    @Operation(operationId = "verifySignupEmail", summary = "Verify email", description = """
                Verify email ownership via the link sent to the requester's email.\
                 Returns an HTML result page.

                **Requirements:**
                - Only available when `email-required` is enabled
                - **Unavailable in OIDC mode**""")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "HTML verification result page",
                content = @Content(mediaType = "text/html")),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @AuthenticationErrors({AuthenticationErrorCode.EMAIL_FEATURE_DISABLED})
    @PublicApi
    @RequireEmail
    @GetMapping("/signup/verify")
    public ModelAndView verifySignupEmail(@RequestParam String token) {
        boolean verified = memberEmailVerificationService.verifyEmail(token);
        String viewName = verified ? "verification-success" : "verification-failure";

        return new ModelAndView(viewName);
    }

    @Operation(operationId = "checkSignupVerification", summary = "Check verification status", description = """
                Poll the current status of an email verification request.

                **Requirements:**
                - Only available when `email-required` is enabled
                - **Unavailable in OIDC mode**""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verification status retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @AuthenticationErrors({AuthenticationErrorCode.EMAIL_FEATURE_DISABLED})
    @PublicApi
    @RequireEmail
    @GetMapping("/signup/status/{verificationId}")
    public ResponseEntity<VerificationStatus> checkSignupVerification(@PathVariable String verificationId) {
        VerificationStatus status = memberEmailVerificationService.getVerificationStatus(verificationId);

        return ResponseEntity.ok(status);
    }
}
