package com.tissue.adapter.web;

import com.tissue.adapter.web.annotation.RequireElevated;
import com.tissue.adapter.web.request.AddPasswordRequest;
import com.tissue.adapter.web.request.UpdateMemberEmailRequest;
import com.tissue.adapter.web.request.UpdateMemberPasswordRequest;
import com.tissue.adapter.web.request.WithdrawMemberRequest;
import com.tissue.application.port.usecase.MemberAccountUseCase;
import com.tissue.principal.MemberDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberAccountController {

    private final MemberAccountUseCase memberAccountUseCase;

    // TODO: 무슨 용도였더라?
    @PostMapping("/password")
    public ResponseEntity<Void> addPassword(
            @Valid @RequestBody AddPasswordRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.addPassword(request.password(), userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

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
            @RequestBody WithdrawMemberRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberAccountUseCase.withdraw(request.password(), userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    // TODO: resetPassword
    //  1. send email with a short-life(15~30 min) token
    //  2. email should have a password reset link
    //  3. change password through that link -> expire token
}
