package com.tissue.api.issue.domain.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.model.Issue;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueProgressService {

	private final List<IssueProgressCalculator> calculators;

	public void calculateAndUpdate(Issue issue) {
		Integer childBased = null;
		Integer pointBased = null;

		for (IssueProgressCalculator calculator : calculators) {
			if (!calculator.supports(issue)) {
				continue;
			}

			Integer progress = calculator.calculate(issue);

			if (calculator.getType() == ProgressType.COUNT_BASED) {
				childBased = progress;
			}
			if (calculator.getType() == ProgressType.POINT_BASED) {
				pointBased = progress;
			}
		}

		issue.updateProgress(childBased, pointBased);
	}

	public void recalculateHierarchy(Issue issue) {
		calculateAndUpdate(issue);

		Optional.ofNullable(issue.getParentIssue())
			.ifPresent(this::recalculateHierarchy);
	}
}
