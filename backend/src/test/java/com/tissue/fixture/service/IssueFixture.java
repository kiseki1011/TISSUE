package com.tissue.fixture.service;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.enums.BugSeverity;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.presentation.dto.request.create.CreateBugRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateEpicRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateStoryRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateSubTaskRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateTaskRequest;
import com.tissue.api.issue.presentation.dto.response.create.CreateIssueResponse;
import com.tissue.api.issue.service.command.IssueCommandService;

@Component
public class IssueFixture {

	@Autowired
	private IssueCommandService issueCommandService;

	public CreateIssueResponse createEpic(
		String workspaceCode,
		String title
	) {
		CreateEpicRequest request = CreateEpicRequest.builder()
			.title(title)
			.content("Epic Content")
			.summary("Epic Summary")
			.dueDate(LocalDate.now())
			.businessGoal("Epic Business Goal")
			.targetReleaseDate(LocalDate.now().plusDays(7))
			.hardDeadLine(LocalDate.now().plusMonths(1))
			.build();

		return issueCommandService.createIssue(workspaceCode, request);
	}

	public CreateIssueResponse createTask(
		String workspaceCode,
		String title,
		String parentIssueKey
	) {
		CreateTaskRequest request = CreateTaskRequest.builder()
			.title(title)
			.content("Task Content")
			.summary("Task Summary")
			.difficulty(Difficulty.NORMAL)
			.dueDate(LocalDate.now())
			.parentIssueKey(parentIssueKey)
			.build();

		return issueCommandService.createIssue(workspaceCode, request);
	}

	public CreateIssueResponse createStory(
		String workspaceCode,
		String title,
		String parentIssueKey
	) {
		CreateStoryRequest request = CreateStoryRequest.builder()
			.title(title)
			.content("Story Content")
			.summary("Story Summary")
			.difficulty(Difficulty.NORMAL)
			.dueDate(LocalDate.now())
			.parentIssueKey(parentIssueKey)
			.userStory("Story User Story")
			.acceptanceCriteria("Story Acceptance Criteria")
			.build();

		return issueCommandService.createIssue(workspaceCode, request);
	}

	public CreateIssueResponse createBug(
		String workspaceCode,
		String title,
		String parentIssueKey
	) {
		CreateBugRequest request = CreateBugRequest.builder()
			.title(title)
			.content("Bug Content")
			.summary("Bug Summary")
			.difficulty(Difficulty.NORMAL)
			.dueDate(LocalDate.now())
			.parentIssueKey(parentIssueKey)
			.reproducingSteps("Bug Reproduce Steps")
			.severity(BugSeverity.MAJOR)
			.affectedVersions(Set.of("1.0.0", "1.0.1"))
			.build();

		return issueCommandService.createIssue(workspaceCode, request);
	}

	public CreateIssueResponse createSubTask(
		String workspaceCode,
		String title,
		String parentIssueKey
	) {
		CreateSubTaskRequest request = CreateSubTaskRequest.builder()
			.title(title)
			.content("SubTask Content")
			.summary("SubTask Summary")
			.difficulty(Difficulty.NORMAL)
			.dueDate(LocalDate.now())
			.parentIssueKey(parentIssueKey)
			.build();

		return issueCommandService.createIssue(workspaceCode, request);
	}
}
