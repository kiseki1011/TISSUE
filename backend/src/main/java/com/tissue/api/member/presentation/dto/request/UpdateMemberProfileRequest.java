package com.tissue.api.member.presentation.dto.request;

import java.time.LocalDate;

import com.tissue.api.common.validator.annotation.pattern.NamePattern;
import com.tissue.api.common.validator.annotation.size.NameSize;
import com.tissue.api.common.validator.annotation.size.text.StandardText;
import com.tissue.api.member.domain.enums.JobType;

import jakarta.validation.constraints.Past;
import lombok.Builder;

@Builder
public record UpdateMemberProfileRequest(
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
	public boolean hasName() {
		return isNotBlank(firstName) && isNotBlank(lastName);
	}

	public boolean hasBirthDate() {
		return birthDate != null;
	}

	public boolean hasJobType() {
		return jobType != null;
	}

	public boolean hasBiography() {
		return isNotBlank(biography);
	}

	private boolean isNotBlank(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
