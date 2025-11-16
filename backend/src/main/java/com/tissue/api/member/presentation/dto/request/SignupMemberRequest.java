package com.tissue.api.member.presentation.dto.request;

import java.time.LocalDate;

import com.tissue.api.common.validator.annotation.pattern.NamePattern;
import com.tissue.api.common.validator.annotation.pattern.PasswordPattern;
import com.tissue.api.common.validator.annotation.pattern.UsernamePattern;
import com.tissue.api.common.validator.annotation.size.EmailSize;
import com.tissue.api.common.validator.annotation.size.NameSize;
import com.tissue.api.common.validator.annotation.size.UsernameSize;
import com.tissue.api.common.validator.annotation.size.password.PasswordSize;
import com.tissue.api.member.application.dto.SignupMemberCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.Builder;

@Builder
public record SignupMemberRequest(
	@EmailSize
	@Email(message = "{valid.pattern.email}}")
	@NotBlank(message = "{valid.notblank}")
	String email,

	@UsernameSize
	@UsernamePattern
	@NotBlank(message = "{valid.notblank}")
	String username,

	@PasswordSize
	@PasswordPattern
	@NotBlank(message = "{valid.notblank}")
	String password,

	@NameSize
	@NamePattern
	String name,

	@Past(message = "{valid.birthdate}")
	LocalDate birthDate
) {
	public SignupMemberCommand toCommand() {
		return SignupMemberCommand.builder()
			.email(email.trim())
			.password(password)
			.username(username.trim())
			.name(name.trim())
			.birthDate(birthDate)
			.build();
	}
}
