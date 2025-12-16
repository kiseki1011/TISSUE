package com.tissue.issue.application.service.finder;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.exception.IssueNotFoundException;
import com.tissue.project.domain.Project;
import com.tissue.sprint.domain.Sprint;
import com.tissue.workflow.domain.enums.StateCategory;
import com.tissue.common.util.IssueKeyUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFinder {

	private final IssueQueryRepository issueQueryRepo;

	// TODO: 이 메서드 사용을 project 사용하는 거로 변경 후 삭제
	public Issue findBy(String issueKey, String workspaceKey) {
		return issueQueryRepo.findByKeyAndWorkspaceKey(issueKey, workspaceKey)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, IssueKeyUtil.extractProjectKey(issueKey), workspaceKey));
	}

	public Issue findBy(Long id) {
		return issueQueryRepo.findById(id)
			.orElseThrow(() -> new RuntimeException("Issue not found"));
	}

	public Issue findBy(String issueKey, Project project) {
		return issueQueryRepo.findByKeyAndProject(issueKey, project)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, project.getKey(), project.getWorkspaceKey()));
	}

	public List<Issue> findAllBy(Collection<String> issueKeys, String workspaceKey) {
		return issueQueryRepo.findByKeyInAndWorkspaceKey(issueKeys, workspaceKey);
	}

	public boolean existsIncompleteIssuesBySprint(Sprint sprint) {
		return issueQueryRepo.existsBySprintAndCategoryNot(sprint, StateCategory.DONE);
	}

	public List<Issue> findIncompleteIssuesBySprint(Sprint sprint) {
		return issueQueryRepo.findIncompleteIssuesBySprint(sprint, StateCategory.DONE);
	}

	public List<String> findIncompleteIssueKeysBySprint(Sprint sprint) {
		return issueQueryRepo.findIncompleteIssueKeysBySprint(sprint, StateCategory.DONE);
	}
}
