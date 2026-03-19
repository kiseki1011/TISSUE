package com.tissue.feature.member.web;

import com.tissue.feature.member.application.dto.MemberProfile;
import com.tissue.feature.member.application.port.usecase.MemberProfileUseCase;
import com.tissue.feature.member.web.request.UpdateMemberLanguageRequest;
import com.tissue.feature.member.web.request.UpdateMemberNameRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            @RequestBody @Valid UpdateMemberNameRequest request, @CurrentMember MemberDetails memberDetails) {

        memberProfileUseCase.updateName(request.newName(), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/language")
    public ResponseEntity<Void> updateMemberLanguage(
            @RequestBody @Valid UpdateMemberLanguageRequest request, @CurrentMember MemberDetails memberDetails) {

        memberProfileUseCase.updateLanguage(request.language(), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    public ResponseEntity<MemberProfile> getMyProfile(@CurrentMember MemberDetails memberDetails) {

        MemberProfile response = memberProfileUseCase.getMyProfile(memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
