package com.tissue.api.member.presentation.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import com.tissue.api.member.domain.Member;

import lombok.Builder;

@Builder
public record SignupMemberResponse(
	Long memberId,
	String loginId,
	String email,
	String biography,
	String name,
	LocalDate birthDate,
	LocalDateTime createdAt
) {

	public static SignupMemberResponse from(Member member) {
		return SignupMemberResponse.builder()
			.memberId(member.getId())
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.biography(member.getBiography())
			.name(member.getName().getLocalizedFullName(Locale.getDefault()))
			.birthDate(member.getBirthDate())
			.createdAt(member.getCreatedDate())
			.build();
	}
}
