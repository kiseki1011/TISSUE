package com.tissue.member.web;

import com.tissue.feature.member.application.dto.MemberProfile;
import com.tissue.feature.member.application.port.usecase.MemberProfileUseCase;
import com.tissue.member.web.request.UpdateMemberLanguageRequest;
import com.tissue.member.web.request.UpdateMemberNameRequest;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberProfileController {

    private final MemberProfileUseCase memberProfileUseCase;

    @PatchMapping("/name")
    public ResponseEntity<Void> updateMemberName(
            @RequestBody @Valid UpdateMemberNameRequest request, @AuthenticationPrincipal MemberDetails userDetails) {
        memberProfileUseCase.updateName(request.newName(), userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/language")
    public ResponseEntity<Void> updateMemberLanguage(
            @RequestBody @Valid UpdateMemberLanguageRequest request,
            @AuthenticationPrincipal MemberDetails userDetails) {
        memberProfileUseCase.updateLanguage(request.language(), userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    public ResponseEntity<MemberProfile> getMyProfile(@CurrentMember MemberDetails userDetails) {
        MemberProfile response = memberProfileUseCase.getMyProfile(userDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
