package com.uranus.taskmanager.api.issue.presentation.dto.request.create;

import java.time.LocalDate;

import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.enums.IssueType;

public interface IssueCreateRequest {

	String title();

	String content();

	String summary();

	IssuePriority priority();

	LocalDate dueDate();

	Long parentIssueId();

	IssueType getType();

}
