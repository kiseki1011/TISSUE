package com.tissue.member.web;

import com.tissue.feature.member.application.dto.response.GetMemberProfile;
import com.tissue.feature.member.application.port.in.MemberQueryUseCase;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberQueryController {

    private final MemberQueryUseCase memberQueryUseCase;

    @GetMapping("/my")
    public ResponseEntity<GetMemberProfile> getMyProfile(@CurrentMember MemberDetails userDetails) {
        GetMemberProfile response = memberQueryUseCase.getMyProfile(userDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
