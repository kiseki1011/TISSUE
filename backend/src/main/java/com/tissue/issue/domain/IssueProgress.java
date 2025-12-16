package com.tissue.issue.domain;

import static com.tissue.common.util.DomainPreconditions.*;

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
}
