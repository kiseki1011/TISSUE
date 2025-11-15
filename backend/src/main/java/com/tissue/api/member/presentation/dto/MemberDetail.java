package com.tissue.api.member.presentation.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.tissue.api.member.domain.model.Member;

import lombok.Builder;

@Builder
public record MemberDetail(
	String email,
	String name,
	LocalDate birthDate,
	Instant createdAt,
	Instant updatedAt
) {
	public static MemberDetail from(Member member) {
		return MemberDetail.builder()
			.email(member.getEmail())
			.name(member.getName())
			.birthDate(member.getBirthDate())
			.createdAt(member.getCreatedAt())
			.updatedAt(member.getLastModifiedAt())
			.build();
	}
}
