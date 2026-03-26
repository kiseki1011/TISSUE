package com.tissue.security.adapter.web;

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

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberAccountController {

    private final MemberAccountUseCase memberAccountUseCase;

    @RequireEmail
    @RequireElevated
    @PostMapping("/link/email")
    public ResponseEntity<Void> linkEmailAuthentication(
            @Valid @RequestBody LinkEmailAuthRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.linkEmailAuthentication(request.password(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @RequireElevated
    @PostMapping("/link/oauth")
    public ResponseEntity<Void> linkOAuthAccount(
            @Valid @RequestBody LinkOAuthAccountRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.linkOAuthAccount(request.registerToken(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @RequireElevated
    @PatchMapping("/username")
    public ResponseEntity<Void> updateMemberUsername(
            @RequestBody @Valid UpdateMemberUsernameRequest request,
            @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.updateUsername(request.newUsername(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @RequireEmail
    @RequireElevated
    @PatchMapping("/email")
    public ResponseEntity<Void> updateMemberEmail(
            @RequestBody @Valid UpdateMemberEmailRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.updateEmail(request.newEmail(), request.verificationToken(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @RequireElevated
    @PatchMapping("/password")
    public ResponseEntity<Void> updateMemberPassword(
            @RequestBody @Valid UpdateMemberPasswordRequest request,
            @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.updatePassword(
                request.originalPassword(), request.newPassword(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @RequireElevated
    @DeleteMapping
    public ResponseEntity<Void> withdrawMember(
            @RequestBody @Valid WithdrawMemberRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.withdraw(request.password(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @RequireEmail
    @GetMapping("/check-email")
    public ResponseEntity<Void> checkEmailAvailability(@RequestParam String email) {
        memberAccountUseCase.checkEmailAvailability(email);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-username")
    public ResponseEntity<Void> checkUsernameAvailability(@RequestParam String username) {
        memberAccountUseCase.checkUsernameAvailability(username);

        return ResponseEntity.noContent().build();
    }
}
