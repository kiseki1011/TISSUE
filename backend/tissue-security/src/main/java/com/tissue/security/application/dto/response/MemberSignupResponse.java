package com.tissue.security.application.dto.response;

import com.tissue.feature.member.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Email signup response")
public record MemberSignupResponse(
        @Schema(description = "ID of the newly created member", example = "1")
        Long memberId) {

    public static MemberSignupResponse from(Member member) {
        return new MemberSignupResponse(member.getId());
    }
}
