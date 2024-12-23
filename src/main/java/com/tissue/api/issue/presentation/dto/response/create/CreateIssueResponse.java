package com.tissue.api.issue.presentation.dto.response.create;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Bug;
import com.tissue.api.issue.domain.types.Epic;
import com.tissue.api.issue.domain.types.Story;
import com.tissue.api.issue.domain.types.SubTask;
import com.tissue.api.issue.domain.types.Task;

public interface CreateIssueResponse {
	IssueType getType();

	static CreateIssueResponse from(Issue issue) {
		return switch (issue.getType()) {
			case EPIC -> CreateEpicResponse.from((Epic)issue);
			case STORY -> CreateStoryResponse.from((Story)issue);
			case TASK -> CreateTaskResponse.from((Task)issue);
			case BUG -> CreateBugResponse.from((Bug)issue);
			case SUB_TASK -> CreateSubTaskResponse.from((SubTask)issue);
		};
	}
}
