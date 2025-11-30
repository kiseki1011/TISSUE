package com.tissue.api.issue.application.service.finder;

import static com.tissue.api.common.util.IssueKeyUtil.*;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.StateCategory;
import com.tissue.api.issue.domain.exception.IssueNotFoundException;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.sprint.domain.Sprint;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFinder {

	private final IssueQueryRepository issueQueryRepo;

	public Issue findBy(String issueKey, String workspaceKey) {
		return issueQueryRepo.findByKeyAndWorkspaceKey(issueKey, workspaceKey)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, extractProjectKey(issueKey), workspaceKey));
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
