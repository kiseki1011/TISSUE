package com.tissue.member.application.dto.response;

import com.tissue.member.domain.Member;
import java.time.Instant;
import lombok.Builder;

@Builder
public record GetMemberProfile(String email, String username, String name, Instant joinedAt, Instant lastModifiedAt) {
    public static GetMemberProfile from(Member member) {
        return GetMemberProfile.builder()
                .email(member.getEmail())
                .username(member.getUsername())
                .name(member.getName())
                .joinedAt(member.getCreatedAt())
                .lastModifiedAt(member.getLastModifiedAt())
                .build();
    }
}
