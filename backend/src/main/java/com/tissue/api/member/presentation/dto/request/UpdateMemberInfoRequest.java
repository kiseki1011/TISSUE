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
	@Past(message = "Birth date must be in the past")
	LocalDate birthDate,
	JobType jobType,
	String introduction

) {
	public boolean hasBirthDate() {
		return birthDate != null;
	}

	public boolean hasJobType() {
		return jobType != null;
	}

	public boolean hasIntroduction() {
		return isNotBlank(introduction);
	}

	private boolean isNotBlank(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
