package com.tissue.api.member.adapter.in.web.dto.request;

import java.time.LocalDate;

import com.tissue.api.common.validator.annotation.pattern.NamePattern;
import com.tissue.api.common.validator.annotation.size.NameSize;

import jakarta.validation.constraints.Past;
import lombok.Builder;

@Builder
public record UpdateMemberProfileRequest(
	@NameSize
	@NamePattern
	String name,

	@Past(message = "{valid.birthdate}")
	LocalDate birthDate

) {
	public boolean hasName() {
		return isNotBlank(name);
	}

	public boolean hasBirthDate() {
		return birthDate != null;
	}

	private boolean isNotBlank(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
