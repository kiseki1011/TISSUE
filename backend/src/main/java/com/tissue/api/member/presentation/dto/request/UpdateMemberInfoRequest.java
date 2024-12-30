package com.tissue.api.member.presentation.dto.request;

import java.time.LocalDate;

import com.tissue.api.member.domain.JobType;

import jakarta.validation.constraints.Past;
import lombok.Builder;

/**
 * Todo
 *  - size 검증 필요
 */
@Builder
public record UpdateMemberInfoRequest(
	String firstName,
	String lastName,
	@Past(message = "Birth date must be in the past")
	LocalDate birthDate,
	JobType jobType,
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
