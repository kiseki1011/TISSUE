package com.tissue.issue.domain;

import org.springframework.lang.Nullable;

import com.tissue.issue.domain.enums.ProgressType;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueProgress {

	private final static int MIN_PERCENTAGE = 0;
	private final static int MAX_PERCENTAGE = 100;

	@Column(name = "count_based_progress")
	private Integer countBasedProgress;

	@Column(name = "point_based_progress")
	private Integer pointBasedProgress;

	static IssueProgress init() {
		return new IssueProgress();
	}

	void update(@Nullable Integer countBased, @Nullable Integer pointBased) {
		this.countBasedProgress = ensureValidPercentageRange(countBased);
		this.pointBasedProgress = ensureValidPercentageRange(pointBased);
	}

	public Integer getByType(ProgressType type) {
		return switch (type) {
			case COUNT_BASED -> countBasedProgress;
			case POINT_BASED -> pointBasedProgress;
		};
	}

	// TODO: should i consider separating this into a separate util class?
	//  currently im only using this here
	private Integer ensureValidPercentageRange(Integer value) {
		if (value == null) {
			return null;
		}
		if (value < MIN_PERCENTAGE || value > MAX_PERCENTAGE) {
			throw new RuntimeException("Percentage must be a value of 0 ~ 100. Input value was %d.".formatted(value));
		}

		return value;
	}
}
