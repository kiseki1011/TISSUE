package com.tissue.member.adapter.in.web;

import com.tissue.member.application.dto.response.GetMemberProfile;
import com.tissue.member.application.port.in.MemberQueryUseCase;
import com.tissue.security.authentication.domain.MemberDetails;
import com.tissue.security.authentication.presentation.annotation.CurrentMember;
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

    // TODO: should i just use the uri as "/api/v1/members"?
    @GetMapping("/my")
    public ResponseEntity<GetMemberProfile> getMyProfile(@CurrentMember MemberDetails userDetails) {
        GetMemberProfile response = memberQueryUseCase.getMyProfile(userDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
