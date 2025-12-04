package com.tissue.api.member.application.dto.response;

import java.time.Instant;
import java.time.LocalDate;

import com.tissue.api.member.domain.Member;

import lombok.Builder;

@Builder
public record GetProfileResponse(
	String email,
	String username,

	String name,
	LocalDate birthDate,

	// int ownedWorkspaceCount,

	Instant joinedAt,
	Instant lastModifiedAt
) {
	public static GetProfileResponse from(Member member) {
		return GetProfileResponse.builder()
			.email(member.getEmail())
			.username(member.getUsername())
			.name(member.getName())
			.birthDate(member.getBirthDate())
			.joinedAt(member.getCreatedAt())
			.lastModifiedAt(member.getLastModifiedAt())
			.build();
	}
}
