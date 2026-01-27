package com.tissue.member.adapter.in.web;

import com.tissue.member.adapter.in.web.dto.request.AddPasswordRequest;
import com.tissue.member.adapter.in.web.dto.request.LinkOAuthAccountRequest;
import com.tissue.member.adapter.in.web.dto.request.SignupMemberRequest;
import com.tissue.member.adapter.in.web.dto.request.SignupOAuthMemberRequest;
import com.tissue.member.adapter.in.web.dto.request.UpdateMemberEmailRequest;
import com.tissue.member.adapter.in.web.dto.request.UpdateMemberLanguageRequest;
import com.tissue.member.adapter.in.web.dto.request.UpdateMemberNameRequest;
import com.tissue.member.adapter.in.web.dto.request.UpdateMemberPasswordRequest;
import com.tissue.member.adapter.in.web.dto.request.UpdateMemberUsernameRequest;
import com.tissue.member.adapter.in.web.dto.request.WithdrawMemberRequest;
import com.tissue.member.application.dto.response.MemberSignupResponse;
import com.tissue.member.application.port.in.MemberCommandUseCase;
import com.tissue.member.application.port.in.MemberQueryUseCase;
import com.tissue.security.authentication.domain.MemberDetails;
import com.tissue.security.authentication.presentation.annotation.RequireElevated;
import com.tissue.security.authentication.presentation.dto.response.OAuthSignupResponse;
import jakarta.validation.Valid;
import java.net.URI;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberCommandUseCase memberCommandUseCase;
    private final MemberQueryUseCase memberQueryUseCase;

    @PostMapping("/signup/email")
    public ResponseEntity<MemberSignupResponse> signup(@Valid @RequestBody SignupMemberRequest request) {
        var command = request.toCommand();
        MemberSignupResponse response = memberCommandUseCase.signup(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{memberId}")
                .buildAndExpand(response.memberId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/signup/oauth")
    public ResponseEntity<OAuthSignupResponse> signupOAuth(@Valid @RequestBody SignupOAuthMemberRequest request) {
        OAuthSignupResponse response = memberCommandUseCase.signupOAuth(request.toCommand());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/link/oauth")
    public ResponseEntity<Void> linkOAuthAccount(
            @Valid @RequestBody LinkOAuthAccountRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberCommandUseCase.linkOAuthAccount(request.registerToken(), userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password")
    public ResponseEntity<Void> addPassword(
            @Valid @RequestBody AddPasswordRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberCommandUseCase.addPassword(request.password(), userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/name")
    public ResponseEntity<Void> updateMemberName(
            @RequestBody @Valid UpdateMemberNameRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberCommandUseCase.updateName(request.newName(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/language")
    public ResponseEntity<Void> updateMemberLanguage(
            @RequestBody @Valid UpdateMemberLanguageRequest request,
            @AuthenticationPrincipal MemberDetails userDetails) {
        memberCommandUseCase.updateLanguage(request.language(), userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @RequireElevated
    @PatchMapping("/email")
    public ResponseEntity<Void> updateMemberEmail(
            @RequestBody @Valid UpdateMemberEmailRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberCommandUseCase.updateEmail(request.newEmail(), request.verificationToken(), userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @RequireElevated
    @PatchMapping("/username")
    public ResponseEntity<Void> updateMemberUsername(
            @RequestBody @Valid UpdateMemberUsernameRequest request,
            @AuthenticationPrincipal MemberDetails userDetails) {
        memberCommandUseCase.updateUsername(request.newUsername(), userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @RequireElevated
    @PatchMapping("/password")
    public ResponseEntity<Void> updateMemberPassword(
            @RequestBody @Valid UpdateMemberPasswordRequest request,
            @AuthenticationPrincipal MemberDetails userDetails) {
        memberCommandUseCase.updatePassword(
                request.originalPassword(), request.newPassword(), userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @RequireElevated
    @DeleteMapping
    public ResponseEntity<Void> withdrawMember(
            @RequestBody WithdrawMemberRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberCommandUseCase.withdraw(request.password(), userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/checkEmail")
    public ResponseEntity<Void> checkEmailAvailability(@RequestParam String email) {
        memberQueryUseCase.checkEmailAvailability(email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/checkUsername")
    public ResponseEntity<Void> checkUsernameAvailability(@RequestParam String username) {
        memberQueryUseCase.checkUsernameAvailability(username);
        return ResponseEntity.noContent().build();
    }

    // TODO: resetPassword
    //  1. send email with a short-life(15~30 min) token
    //  2. email should have a password reset link
    //  3. change password through that link -> expire token
}
