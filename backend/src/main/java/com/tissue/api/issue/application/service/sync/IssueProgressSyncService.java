package com.tissue.api.issue.application.service.sync;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.application.service.calculator.IssueProgressCalculator;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.ProgressType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueProgressSyncService {

	private final List<IssueProgressCalculator> calculators;

	public void recalculateAndUpdate(Issue issue) {
		if (issue == null) {
			return;
		}

		Integer childBased = null;
		Integer pointBased = null;

		for (IssueProgressCalculator calculator : calculators) {
			Integer progress = calculator.calculate(issue);

			if (progress == null) {
				continue;
			}

			if (calculator.getType() == ProgressType.COUNT_BASED) {
				childBased = progress;
			}
			if (calculator.getType() == ProgressType.POINT_BASED) {
				pointBased = progress;
			}
		}

		issue.updateProgress(childBased, pointBased);
	}

	public void recalculateProgress(Issue issue) {
		Optional.ofNullable(issue.getParentIssue())
			.ifPresent(this::recalculateAndUpdate);
	}
}
