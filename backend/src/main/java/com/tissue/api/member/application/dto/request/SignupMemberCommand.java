package com.tissue.api.member.application.dto.request;

import java.time.LocalDate;

import lombok.Builder;

@Builder
public record SignupMemberCommand(
	String email,
	String username,
	String password,
	String name,
	LocalDate birthDate
) {
	// public Member toEntity(String encodedPassword) {
	// 	return Member.builder()
	// 		.email(email)
	// 		.password(encodedPassword)
	// 		.username(username)
	// 		.name(name)
	// 		.birthDate(birthDate)
	// 		.build();
	// }
}
