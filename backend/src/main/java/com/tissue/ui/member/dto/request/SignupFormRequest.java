package com.tissue.ui.member.dto.request;

import java.time.LocalDate;

import com.tissue.api.common.validator.annotation.pattern.IdPattern;
import com.tissue.api.common.validator.annotation.pattern.NamePattern;
import com.tissue.api.common.validator.annotation.pattern.PasswordPattern;
import com.tissue.api.common.validator.annotation.pattern.UsernamePattern;
import com.tissue.api.common.validator.annotation.size.EmailSize;
import com.tissue.api.common.validator.annotation.size.IdSize;
import com.tissue.api.common.validator.annotation.size.NameSize;
import com.tissue.api.common.validator.annotation.size.UsernameSize;
import com.tissue.api.common.validator.annotation.size.password.PasswordSize;
import com.tissue.api.member.application.dto.SignupMemberCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.Builder;

@Builder
public record SignupFormRequest(

	@IdSize @IdPattern @NotBlank String loginId,

	@EmailSize @Email @NotBlank String email,

	@UsernameSize @UsernamePattern @NotBlank String username,

	@PasswordSize @PasswordPattern @NotBlank String password,

	@NameSize @NamePattern String name,

	@Past LocalDate birthDate
) {
	// TODO: email, username, profile(name, birthdate) vo 만들어서 사용
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
