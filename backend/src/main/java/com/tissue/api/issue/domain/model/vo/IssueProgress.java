package com.tissue.api.issue.domain.model.vo;

import org.springframework.lang.Nullable;

import com.tissue.api.issue.domain.service.ProgressType;

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

	public void update(@Nullable Integer countBased, @Nullable Integer pointBased) {
		this.countBasedProgress = countBased;
		this.pointBasedProgress = pointBased;
	}

	public Integer getByType(ProgressType type) {
		return switch (type) {
			case COUNT_BASED -> countBasedProgress;
			case POINT_BASED -> pointBasedProgress;
		};
	}
}
