package com.tissue.issue.application.service.finder;

import static com.tissue.workflow.domain.enums.StateCategory.*;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.exception.IssueExceptions;
import com.tissue.project.domain.Project;
import com.tissue.sprint.domain.Sprint;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFinder {

	private final IssueQueryRepository issueQueryRepo;

	public Issue findBy(String issueKey, Project project) {
		return issueQueryRepo.findByKeyAndProject(issueKey, project)
			.orElseThrow(() -> IssueExceptions.notFound(project.getWorkspaceKey(), issueKey));
	}

	public List<Issue> findAllBy(Collection<String> issueKeys, String workspaceKey) {
		return issueQueryRepo.findByKeyInAndWorkspaceKey(issueKeys, workspaceKey);
	}

	public List<Issue> findIncompleteIssuesBySprint(Sprint sprint) {
		return issueQueryRepo.findIncompleteIssuesBySprint(sprint, COMPLETED);
	}

	public List<String> findIncompleteIssueKeysBySprint(Sprint sprint) {
		return issueQueryRepo.findIncompleteIssueKeysBySprint(sprint, COMPLETED);
	}
}
