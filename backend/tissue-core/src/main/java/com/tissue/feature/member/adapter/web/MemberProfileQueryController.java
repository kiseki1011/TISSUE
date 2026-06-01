package com.tissue.feature.member.adapter.web;

import com.tissue.feature.member.application.dto.MemberProfile;
import com.tissue.feature.member.application.port.usecase.MemberProfileQueryUseCase;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member Profile")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberProfileQueryController {

    private final MemberProfileQueryUseCase memberProfileQueryUseCase;

    @Operation(operationId = "getMyProfile", summary = "Get my profile", description = """
                    Get the current user's profile.

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.MEMBER_NOT_FOUND, MemberErrorCode.MEMBER_DELETED})
    @GetMapping("/me")
    public ResponseEntity<MemberProfile> getMyProfile(@CurrentMember MemberDetails memberDetails) {
        MemberProfile response = memberProfileQueryUseCase.getMyProfile(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
