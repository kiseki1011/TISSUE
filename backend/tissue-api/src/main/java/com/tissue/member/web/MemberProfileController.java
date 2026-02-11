package com.tissue.member.web;

import com.tissue.feature.member.application.port.in.MemberCommandUseCase;
import com.tissue.feature.member.application.port.in.MemberQueryUseCase;
import com.tissue.member.web.request.UpdateMemberLanguageRequest;
import com.tissue.member.web.request.UpdateMemberNameRequest;
import com.tissue.member.web.request.UpdateMemberUsernameRequest;
import com.tissue.principal.MemberDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberProfileController {

    private final MemberCommandUseCase memberCommandUseCase;
    private final MemberQueryUseCase memberQueryUseCase;

    @PatchMapping("/username")
    public ResponseEntity<Void> updateMemberUsername(
            @RequestBody @Valid UpdateMemberUsernameRequest request,
            @AuthenticationPrincipal MemberDetails userDetails) {
        memberCommandUseCase.updateUsername(request.newUsername(), userDetails.getMemberId());
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

    @GetMapping("/check-email")
    public ResponseEntity<Void> checkEmailAvailability(@RequestParam String email) {
        memberQueryUseCase.checkEmailAvailability(email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-username")
    public ResponseEntity<Void> checkUsernameAvailability(@RequestParam String username) {
        memberQueryUseCase.checkUsernameAvailability(username);
        return ResponseEntity.noContent().build();
    }
}
