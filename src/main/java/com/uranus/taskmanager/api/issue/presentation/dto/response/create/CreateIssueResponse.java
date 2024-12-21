package com.uranus.taskmanager.api.issue.presentation.dto.response.create;

import com.uranus.taskmanager.api.issue.domain.Issue;
import com.uranus.taskmanager.api.issue.domain.enums.IssueType;
import com.uranus.taskmanager.api.issue.domain.types.Bug;
import com.uranus.taskmanager.api.issue.domain.types.Epic;
import com.uranus.taskmanager.api.issue.domain.types.Story;
import com.uranus.taskmanager.api.issue.domain.types.SubTask;
import com.uranus.taskmanager.api.issue.domain.types.Task;

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
