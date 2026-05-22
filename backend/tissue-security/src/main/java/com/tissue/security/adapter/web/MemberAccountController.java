package com.tissue.security.adapter.web;

import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.global.openapi.AuthenticationErrors;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.security.adapter.web.annotation.PublicApi;
import com.tissue.security.adapter.web.annotation.RequireElevated;
import com.tissue.security.adapter.web.annotation.RequireEmail;
import com.tissue.security.adapter.web.request.LinkEmailAuthRequest;
import com.tissue.security.adapter.web.request.LinkOAuthAccountRequest;
import com.tissue.security.adapter.web.request.RestoreMemberRequest;
import com.tissue.security.adapter.web.request.UpdateMemberEmailRequest;
import com.tissue.security.adapter.web.request.UpdateMemberPasswordRequest;
import com.tissue.security.adapter.web.request.UpdateMemberUsernameRequest;
import com.tissue.security.adapter.web.request.WithdrawMemberRequest;
import com.tissue.security.application.port.usecase.MemberAccountUseCase;
import com.tissue.security.domain.exception.AuthenticationErrorCode;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member Account")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberAccountController {

    private final MemberAccountUseCase memberAccountUseCase;

    @Operation(operationId = "linkEmailAuthentication", summary = "Link email authentication", description = """
                Add email/password authentication to an existing account. \
                (For accounts registered with OAuth or username.)

                **Requirements:**
                - Requires an elevated token (`POST /api/v1/auth/token:elevate`)
                - Only available when `email-required` is enabled""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Email authentication linked"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @AuthenticationErrors({
        AuthenticationErrorCode.EMAIL_FEATURE_DISABLED,
        AuthenticationErrorCode.EMAIL_IDENTITY_ALREADY_EXISTS,
    })
    @MemberErrors({
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_DELETED,
    })
    @RequireEmail
    @RequireElevated
    @PostMapping("/members/link/email")
    public ResponseEntity<Void> linkEmailAuthentication(
            @RequestBody @Valid LinkEmailAuthRequest request, @CurrentMember MemberDetails memberDetails) {
        memberAccountUseCase.linkEmailAuthentication(request.password(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "linkOAuthAccount", summary = "Link OAuth account", description = """
                Link an OAuth provider account to the current member.

                **Requirements:**
                - Requires an elevated token (`POST /api/v1/auth/token:elevate`)""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "OAuth account linked"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @AuthenticationErrors({
        AuthenticationErrorCode.INVALID_TOKEN,
        AuthenticationErrorCode.EXPIRED_TOKEN,
        AuthenticationErrorCode.OAUTH_IDENTITY_ALREADY_LINKED,
    })
    @MemberErrors({
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_DELETED,
    })
    @RequireElevated
    @PostMapping("/members/link/oauth")
    public ResponseEntity<Void> linkOAuthAccount(
            @RequestBody @Valid LinkOAuthAccountRequest request, @CurrentMember MemberDetails memberDetails) {
        memberAccountUseCase.linkOAuthAccount(request.registerToken(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "updateMemberUsername", summary = "Update username", description = """
                Change the current member's username.

                **Requirements:**
                - Requires an elevated token (`POST /api/v1/auth/token:elevate`)
                - `newUsername` must be unique""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Username updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_DELETED,
        MemberErrorCode.DUPLICATE_USERNAME,
    })
    @RequireElevated
    @PatchMapping("/members/username")
    public ResponseEntity<Void> updateMemberUsername(
            @RequestBody @Valid UpdateMemberUsernameRequest request, @CurrentMember MemberDetails memberDetails) {
        memberAccountUseCase.updateUsername(request.newUsername(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "updateMemberEmail", summary = "Update email", description = """
                Change the current member's email address.

                **Requirements:**
                - Requires an elevated token (`POST /api/v1/auth/token:elevate`)
                - Requires a verified email token
                - `newEmail` must be unique
                - Only available when `email-required` is enabled""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Email updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @AuthenticationErrors({
        AuthenticationErrorCode.EMAIL_FEATURE_DISABLED,
        AuthenticationErrorCode.EMAIL_NOT_VERIFIED,
    })
    @MemberErrors({
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_DELETED,
        MemberErrorCode.DUPLICATE_EMAIL,
    })
    @RequireEmail
    @RequireElevated
    @PatchMapping("/members/email")
    public ResponseEntity<Void> updateMemberEmail(
            @RequestBody @Valid UpdateMemberEmailRequest request, @CurrentMember MemberDetails memberDetails) {
        memberAccountUseCase.updateEmail(request.newEmail(), request.verificationToken(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "updateMemberPassword", summary = "Update password", description = """
                Change the current member's password.

                **Requirements:**
                - Requires an elevated token (`POST /api/v1/auth/token:elevate`)""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Password updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @AuthenticationErrors({AuthenticationErrorCode.EMAIL_AUTHENTICATION_IDENTITY_NOT_FOUND})
    @MemberErrors({
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_DELETED,
    })
    @RequireElevated
    @PatchMapping("/members/password")
    public ResponseEntity<Void> updateMemberPassword(
            @RequestBody @Valid UpdateMemberPasswordRequest request, @CurrentMember MemberDetails memberDetails) {
        memberAccountUseCase.updatePassword(
                request.originalPassword(), request.newPassword(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "withdrawMember", summary = "Withdraw account", description = """
                Change the status of the current member's account to `DELETED`.

                **Requirements:**
                - Requires an elevated token (`POST /api/v1/auth/token:elevate`)""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Account deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @AuthenticationErrors({AuthenticationErrorCode.OWNER_NOT_WITHDRAWABLE})
    @MemberErrors({
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_DELETED,
    })
    @RequireElevated
    @DeleteMapping("/members")
    public ResponseEntity<Void> withdrawMember(
            @RequestBody @Valid WithdrawMemberRequest request, @CurrentMember MemberDetails memberDetails) {
        memberAccountUseCase.withdraw(request.password(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "restoreMember", summary = "Restore a withdrawn account", description = """
                Reverse a pending account deletion while still within the configured retention window.

                **Requirements:**
                - No login required (authenticates via the same credentials used for login)
                - Account must be in `DELETED` status and within retention""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Account restored"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @AuthenticationErrors({
        AuthenticationErrorCode.RESTORE_INVALID_CREDENTIALS,
        AuthenticationErrorCode.RESTORE_NOT_DELETED
    })
    @PublicApi
    @PostMapping("/members:restore")
    public ResponseEntity<Void> restoreMember(@RequestBody @Valid RestoreMemberRequest request) {
        memberAccountUseCase.restore(request.identifier(), request.password());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "checkEmailAvailability", summary = "Check email availability", description = """
                Check whether an email address is available for registration.

                **Requirements:**
                - Only available when `email-required` is enabled""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Email is available"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @AuthenticationErrors({AuthenticationErrorCode.EMAIL_FEATURE_DISABLED})
    @MemberErrors({MemberErrorCode.DUPLICATE_EMAIL})
    @PublicApi
    @RequireEmail
    @GetMapping("/members:checkEmail")
    public ResponseEntity<Void> checkEmailAvailability(
            @Parameter(description = "Email address to check") @RequestParam String email) {
        memberAccountUseCase.checkEmailAvailability(email);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "checkUsernameAvailability",
            summary = "Check username availability",
            description = "Check whether a username is available for registration.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Username is available"),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.DUPLICATE_USERNAME})
    @PublicApi
    @GetMapping("/members:checkUsername")
    public ResponseEntity<Void> checkUsernameAvailability(
            @Parameter(description = "Username to check") @RequestParam String username) {
        memberAccountUseCase.checkUsernameAvailability(username);

        return ResponseEntity.noContent().build();
    }
}
