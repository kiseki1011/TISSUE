package com.tissue.api.issue.domain.model.vo;

import static com.tissue.api.common.util.DomainPreconditions.*;

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

	// TODO: nullable=false로 하고, 그냥 null이면 0으로 취급하도록 할까? 현재 상황은 다음과 같음.
	//  - countBased의 경우에는 자식이 없으면 null
	//  - pointBased의 경우에는 자식이 없거나 storyPoint를 설정한 자식이 없는 경우에 null
	@Column(name = "count_based_progress")
	private Integer countBasedProgress;

	@Column(name = "point_based_progress")
	private Integer pointBasedProgress;

	public static IssueProgress init() {
		return new IssueProgress();
	}

	public void update(@Nullable Integer countBased, @Nullable Integer pointBased) {
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
