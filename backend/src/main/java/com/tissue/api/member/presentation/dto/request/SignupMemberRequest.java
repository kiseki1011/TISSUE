package com.tissue.api.member.presentation.dto.request;

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
import com.tissue.api.common.validator.annotation.size.text.StandardText;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.domain.model.enums.JobType;
import com.tissue.api.member.domain.vo.Name;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.Builder;

@Builder
public record SignupMemberRequest(
	@IdSize
	@IdPattern
	@NotBlank(message = "{valid.notblank}")
	String loginId,

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
	String firstName,

	@NameSize
	@NamePattern
	String lastName,

	@Past(message = "{valid.birthdate}")
	LocalDate birthDate,

	JobType jobType,

	@StandardText
	String biography
) {
	public Member toEntity(String encodedPassword) {
		return Member.builder()
			.loginId(this.loginId)
			.email(this.email)
			.password(encodedPassword)
			.username(this.username)
			.name(Name.builder()
				.firstName(this.firstName)
				.lastName(this.lastName)
				.build())
			.birthDate(this.birthDate)
			.jobType(this.jobType)
			.biography(this.biography)
			.build();
	}
}
