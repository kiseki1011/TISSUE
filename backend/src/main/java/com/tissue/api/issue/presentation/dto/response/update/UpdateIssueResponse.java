package com.tissue.api.issue.presentation.dto.response.update;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Bug;
import com.tissue.api.issue.domain.types.Epic;
import com.tissue.api.issue.domain.types.Story;
import com.tissue.api.issue.domain.types.SubTask;
import com.tissue.api.issue.domain.types.Task;

/**
 *  Todo
 *   - switch문 default에 예외 발생 추가 -> UpdateIssueResponseTypeConversionException이 적당할 듯
 *   - 서버 오류로 취급 ㄱㄱ
 */
public interface UpdateIssueResponse {

	static UpdateIssueResponse from(Issue issue) {
		return switch (issue.getType()) {
			case EPIC -> UpdateEpicResponse.from((Epic)issue);
			case STORY -> UpdateStoryResponse.from((Story)issue);
			case TASK -> UpdateTaskResponse.from((Task)issue);
			case BUG -> UpdateBugResponse.from((Bug)issue);
			case SUB_TASK -> UpdateSubTaskResponse.from((SubTask)issue);
		};
	}

	IssueType getType();
}
