package com.tissue.api.issue.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.exception.CannotDeleteParentOfSubTaskException;
import com.tissue.api.issue.exception.IssueTypeMismatchException;
import com.tissue.api.issue.presentation.dto.request.update.UpdateIssueRequest;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueValidator {

	public void validateIssueTypeMatch(Issue issue, UpdateIssueRequest request) {
		if (issue.getType() != request.getType()) {
			throw new IssueTypeMismatchException();
		}
	}

	/**
	 * Todo
	 *  - 지연 로딩으로 childIssue를 메모리에 전부 부르기 때문에, 하위 자식 이슈가 많으면 성능 이슈
	 *  - 이 경우 그냥 레포지토리에서 자식 이슈 중 SUB_TASK가 존재하는지 검증하는 SQL 사용해서 최적화 ㄱㄱ
	 */
	public void validateNotParentOfSubTask(Issue issue) {
		boolean hasSubTask = issue.getChildIssues().stream()
			.anyMatch(childIssue -> childIssue.getType() == IssueType.SUB_TASK);

		if (hasSubTask) {
			throw new CannotDeleteParentOfSubTaskException();
		}
	}
}
