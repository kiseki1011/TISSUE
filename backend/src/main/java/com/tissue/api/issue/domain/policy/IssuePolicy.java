package com.tissue.api.issue.domain.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.BadRequestException;
import com.tissue.api.issue.domain.Issue;

import lombok.Getter;

// TODO: IssuePolicyConfig를 만들어서 해당 config에서 값을 주입하는 방식 권장
@Component
@Getter
public class IssuePolicy {

	@Value("${issue.max-reviewers:10}")
	private int maxReviewers;

	public void ensureCanAddReviewer(int currentCount) {
		if (currentCount >= maxReviewers) {
			throw new BadRequestException("The max number of reviewers is " + maxReviewers);
		}
	}

	public void ensureCanAddReviewer(Issue issue) {
		if (issue.getParticipants().getReviewers().size() >= maxReviewers) {
			throw new BadRequestException("The max number of reviewers is " + maxReviewers);
		}
	}
}
