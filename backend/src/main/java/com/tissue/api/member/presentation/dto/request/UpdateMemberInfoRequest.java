package com.tissue.api.member.presentation.dto.request;

import java.time.LocalDate;

import com.tissue.api.member.domain.JobType;

import jakarta.validation.constraints.Past;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateMemberInfoRequest {

	/**
	 * Todo
	 *  - size 검증 필요
	 */
	@Past(message = "Birth date must be in the past")
	private LocalDate birthDate;
	private JobType jobType;
	private String introduction;

	@Builder
	public UpdateMemberInfoRequest(
		LocalDate birthDate,
		JobType jobType,
		String introduction
	) {
		this.birthDate = birthDate;
		this.jobType = jobType;
		this.introduction = introduction;
	}

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
