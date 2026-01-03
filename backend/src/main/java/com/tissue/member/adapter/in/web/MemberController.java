package com.tissue.member.adapter.in.web;

import com.tissue.member.adapter.in.web.dto.request.SignupMemberRequest;
import com.tissue.member.adapter.in.web.dto.request.UpdateMemberEmailRequest;
import com.tissue.member.adapter.in.web.dto.request.UpdateMemberNameRequest;
import com.tissue.member.adapter.in.web.dto.request.UpdateMemberPasswordRequest;
import com.tissue.member.adapter.in.web.dto.request.UpdateMemberUsernameRequest;
import com.tissue.member.adapter.in.web.dto.request.WithdrawMemberRequest;
import com.tissue.member.application.dto.response.MemberSignupResponse;
import com.tissue.member.application.port.in.MemberCommandUseCase;
import com.tissue.member.application.port.in.MemberQueryUseCase;
import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.presentation.annotation.RequireElevated;
import com.tissue.security.authentication.resolver.CurrentMember;
import jakarta.validation.Valid;
import java.net.URI;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

// TODO: consdier OAuth
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberCommandUseCase memberCommandUseCase;
    private final MemberQueryUseCase memberQueryUseCase;

    @PostMapping
    public ResponseEntity<MemberSignupResponse> signup(@Valid @RequestBody SignupMemberRequest request) {
        var command = request.toCommand();
        MemberSignupResponse response = memberCommandUseCase.signup(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{memberId}")
                .buildAndExpand(response.memberId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/name")
    public ResponseEntity<Void> updateMemberName(
            @RequestBody @Valid UpdateMemberNameRequest request, @CurrentMember MemberUserDetails userDetails) {
        memberCommandUseCase.updateName(request.newName(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    // TODO: consider 2-factor
    @RequireElevated
    @PatchMapping("/email")
    public ResponseEntity<Void> updateMemberEmail(
            @RequestBody @Valid UpdateMemberEmailRequest request, @CurrentMember MemberUserDetails userDetails) {
        memberCommandUseCase.updateEmail(request.newEmail(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @RequireElevated
    @PatchMapping("/username")
    public ResponseEntity<Void> updateMemberUsername(
            @RequestBody @Valid UpdateMemberUsernameRequest request, @CurrentMember MemberUserDetails userDetails) {
        memberCommandUseCase.updateUsername(request.newUsername(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    // TODO: consider 2-factor
    @RequireElevated
    @PatchMapping("/password")
    public ResponseEntity<Void> updateMemberPassword(
            @RequestBody @Valid UpdateMemberPasswordRequest request, @CurrentMember MemberUserDetails userDetails) {
        memberCommandUseCase.updatePassword(
                request.originalPassword(), request.newPassword(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @RequireElevated
    @DeleteMapping
    public ResponseEntity<Void> withdrawMember(
            @RequestBody WithdrawMemberRequest request, @CurrentMember MemberUserDetails userDetails) {
        memberCommandUseCase.withdraw(request.password(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    // TODO: resetPassword
    //  1. send email with a short-life(15~30 min) token
    //  2. email should have a password reset link
    //  3. change password through that link -> expire token

    /** Check email uniqueness */
    @GetMapping("/checkEmail")
    public ResponseEntity<Void> checkEmailAvailability(@RequestParam String email) {
        // Refactored: now uses the query use case instead of direct validator call
        memberQueryUseCase.checkEmailAvailability(email);
        return ResponseEntity.noContent().build();
    }

    /** Check username uniqueness */
    @GetMapping("/checkUsername")
    public ResponseEntity<Void> checkUsernameAvailability(@RequestParam String username) {
        memberQueryUseCase.checkUsernameAvailability(username);
        return ResponseEntity.noContent().build();
    }
}
