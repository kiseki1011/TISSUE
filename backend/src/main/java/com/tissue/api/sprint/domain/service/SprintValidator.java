package com.tissue.api.sprint.domain.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.project.domain.Project;
import com.tissue.api.sprint.application.service.finder.SprintFinder;
import com.tissue.api.sprint.domain.model.Sprint;
import com.tissue.api.sprint.exception.ActiveSprintExistsException;
import com.tissue.api.sprint.exception.IncompleteSprintIssuesFoundException;
import com.tissue.api.sprint.exception.SprintClosedException;
import com.tissue.api.sprint.exception.SprintIssueProjectMismatchException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SprintValidator {

	private final SprintFinder sprintFinder;

	public void ensureIssueInSprintProject(Issue issue, Project project) {
		if (!issue.getProjectKey().equals(project.getKey())) {
			throw new SprintIssueProjectMismatchException(
				issue.getKey(),
				issue.getProjectKey(),
				project.getKey()
			);
		}
	}

	public void ensureAllIssuesCompleted(List<String> incompleteIssueKeys, Sprint sprint, Project project) {
		if (!incompleteIssueKeys.isEmpty()) {
			throw new IncompleteSprintIssuesFoundException(incompleteIssueKeys, sprint.getId(), project.getKey());
		}
	}

	public void ensureSprintNotClosed(Sprint sprint, Project project) {
		if (sprint.isCompleted()) {
			throw new SprintClosedException(sprint.getId(), project.getKey(), project.getWorkspaceKey());
		}
	}

	public void ensureNoActiveSprint(Project project) {
		sprintFinder.findActiveBy(project).ifPresent(activeSprint -> {
			throw new ActiveSprintExistsException(
				project.getKey(),
				activeSprint.getId(),
				activeSprint.getTitle()
			);
		});
	}

}
