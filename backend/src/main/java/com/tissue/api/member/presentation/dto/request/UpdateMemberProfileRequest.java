package com.tissue.api.member.presentation.dto.request;

import java.time.LocalDate;

import com.tissue.api.common.validator.annotation.pattern.NamePattern;
import com.tissue.api.common.validator.annotation.size.NameSize;
import com.tissue.api.member.domain.model.enums.JobType;

import jakarta.validation.constraints.Past;
import lombok.Builder;

// TODO: 각 필드 업데이트를 위한 요청으로 전부 쪼개서 사용? 할 필요는 없을듯..
@Builder
public record UpdateMemberProfileRequest(
	@NameSize
	@NamePattern
	String name,

	@Past(message = "{valid.birthdate}")
	LocalDate birthDate,

	JobType jobType
) {
	public boolean hasName() {
		return isNotBlank(name);
	}

	public boolean hasBirthDate() {
		return birthDate != null;
	}

	public boolean hasJobType() {
		return jobType != null;
	}

	private boolean isNotBlank(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
