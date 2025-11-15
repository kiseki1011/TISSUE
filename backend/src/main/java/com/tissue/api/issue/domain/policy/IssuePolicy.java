package com.tissue.api.issue.domain.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
			// TODO: IssueReviewerLimitExceededException
			throw new RuntimeException("The max number of reviewers is " + maxReviewers);
		}
	}

	// TODO: 아래와 같은 방식 말고 위의 메서드와 같이 currentCount를 파라미터로 받는 방식으로 변경할까?
	public void ensureCanAddReviewer(Issue issue) {
		// TODO: IssueReviewerLimitExceededException
		if (issue.getParticipants().getReviewers().size() >= maxReviewers) {
			throw new RuntimeException("The max number of reviewers is " + maxReviewers);
		}
	}
}
