package com.tissue.security.adapter.web;

import com.tissue.security.adapter.web.annotation.PublicApi;
import com.tissue.security.adapter.web.annotation.RequireElevated;
import com.tissue.security.adapter.web.annotation.RequireEmail;
import com.tissue.security.adapter.web.request.LinkEmailAuthRequest;
import com.tissue.security.adapter.web.request.LinkOAuthAccountRequest;
import com.tissue.security.adapter.web.request.UpdateMemberEmailRequest;
import com.tissue.security.adapter.web.request.UpdateMemberPasswordRequest;
import com.tissue.security.adapter.web.request.UpdateMemberUsernameRequest;
import com.tissue.security.adapter.web.request.WithdrawMemberRequest;
import com.tissue.security.application.port.usecase.MemberAccountUseCase;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberAccountController {

    private final MemberAccountUseCase memberAccountUseCase;

    @Operation(summary = "Link email authentication", description = """
                Add email/password authentication to an existing account. \
                (For accounts registered with OAuth or username.)

                **Requirements:**
                - Requires an elevated token
                - Only available when `email-required` is enabled""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Email authentication linked"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid or missing elevated token", content = @Content),
        @ApiResponse(responseCode = "409", description = "Email authentication already linked", content = @Content)
    })
    @RequireEmail
    @RequireElevated
    @PostMapping("/link/email")
    public ResponseEntity<Void> linkEmailAuthentication(
            @Valid @RequestBody LinkEmailAuthRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.linkEmailAuthentication(request.password(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Link OAuth account", description = """
                Link an OAuth provider account to the current member.

                **Requirements:**
                - Requires an elevated token""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "OAuth account linked"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid or missing elevated token", content = @Content),
        @ApiResponse(responseCode = "409", description = "OAuth account already linked", content = @Content)
    })
    @RequireElevated
    @PostMapping("/link/oauth")
    public ResponseEntity<Void> linkOAuthAccount(
            @Valid @RequestBody LinkOAuthAccountRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.linkOAuthAccount(request.registerToken(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update username", description = """
                Change the current member's username.

                **Requirements:**
                - Requires an elevated token
                - `newUsername` must be unique""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Username updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid or missing elevated token", content = @Content),
        @ApiResponse(responseCode = "409", description = "Username already taken", content = @Content)
    })
    @RequireElevated
    @PatchMapping("/username")
    public ResponseEntity<Void> updateMemberUsername(
            @RequestBody @Valid UpdateMemberUsernameRequest request,
            @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.updateUsername(request.newUsername(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update email", description = """
                Change the current member's email address.

                **Requirements:**
                - Requires an elevated token
                - Requires a verified email token
                - `newEmail` must be unique
                - Only available when `email-required` is enabled""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Email updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid or missing elevated token", content = @Content),
        @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content)
    })
    @RequireEmail
    @RequireElevated
    @PatchMapping("/email")
    public ResponseEntity<Void> updateMemberEmail(
            @RequestBody @Valid UpdateMemberEmailRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.updateEmail(request.newEmail(), request.verificationToken(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update password", description = """
                Change the current member's password.

                **Requirements:**
                - Requires an elevated token""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Password updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid or missing elevated token", content = @Content),
        @ApiResponse(
                responseCode = "404",
                description = "Email/username authentication provider not registered",
                content = @Content)
    })
    @RequireElevated
    @PatchMapping("/password")
    public ResponseEntity<Void> updateMemberPassword(
            @RequestBody @Valid UpdateMemberPasswordRequest request,
            @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.updatePassword(
                request.originalPassword(), request.newPassword(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Withdraw account", description = """
                Delete the current member's account.

                **Requirements:**
                - Requires an elevated token""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Account deleted"),
        @ApiResponse(responseCode = "400", description = "Cannot withdraw while owning workspaces", content = @Content),
        @ApiResponse(responseCode = "401", description = "Invalid or missing elevated token", content = @Content)
    })
    @RequireElevated
    @DeleteMapping
    public ResponseEntity<Void> withdrawMember(
            @RequestBody @Valid WithdrawMemberRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.withdraw(request.password(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check email availability", description = """
                Check whether an email address is available for registration.

                **Requirements:**
                - Only available when `email-required` is enabled""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Email is available"),
        @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content)
    })
    @PublicApi
    @RequireEmail
    @GetMapping("/check-email")
    public ResponseEntity<Void> checkEmailAvailability(@RequestParam String email) {
        memberAccountUseCase.checkEmailAvailability(email);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Check username availability",
            description = "Check whether a username is available for registration.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Username is available"),
        @ApiResponse(responseCode = "409", description = "Username already taken", content = @Content)
    })
    @PublicApi
    @GetMapping("/check-username")
    public ResponseEntity<Void> checkUsernameAvailability(@RequestParam String username) {
        memberAccountUseCase.checkUsernameAvailability(username);

        return ResponseEntity.noContent().build();
    }
}
