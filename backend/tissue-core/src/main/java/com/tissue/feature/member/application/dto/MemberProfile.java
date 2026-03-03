package com.tissue.feature.member.application.dto;

import com.tissue.feature.member.domain.Member;
import java.time.Instant;
import lombok.Builder;

@Builder
public record MemberProfile(String email, String username, String name, Instant joinedAt, Instant lastModifiedAt) {
    public static MemberProfile from(Member member) {
        return MemberProfile.builder()
                .email(member.getEmail())
                .username(member.getUsername())
                .name(member.getName())
                .joinedAt(member.getCreatedAt())
                .lastModifiedAt(member.getLastModifiedAt())
                .build();
    }
}
