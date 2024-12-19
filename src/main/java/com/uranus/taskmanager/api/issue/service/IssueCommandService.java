package com.uranus.taskmanager.api.issue.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.issue.domain.Issue;
import com.uranus.taskmanager.api.issue.domain.repository.IssueRepository;
import com.uranus.taskmanager.api.issue.exception.IssueNotFoundException;
import com.uranus.taskmanager.api.issue.presentation.dto.request.CreateIssueRequest;
import com.uranus.taskmanager.api.issue.presentation.dto.request.UpdateStatusRequest;
import com.uranus.taskmanager.api.issue.presentation.dto.response.CreateIssueResponse;
import com.uranus.taskmanager.api.issue.presentation.dto.response.UpdateStatusResponse;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class IssueCommandService {

	private final IssueRepository issueRepository;
	private final WorkspaceRepository workspaceRepository;

	@Transactional
	public CreateIssueResponse createIssue(
		String code,
		CreateIssueRequest request
	) {
		/*
		 * Todo
		 *  - 생각해보면, 이미 RoleRequired에 대한 인터셉터에서 Workspace를 검증하는데 여기서 또 검증이 필요한가?
		 *  - RequestContextHolder에 대해 찾아보기
		 */
		Workspace workspace = workspaceRepository.findByCode(code)
			.orElseThrow(WorkspaceNotFoundException::new);

		Issue issue = request.to(
			workspace,
			findParentIssue(request.parentIssueId(), code).orElse(null)
		);

		issueRepository.save(issue);

		return CreateIssueResponse.from(issue);
	}

	@Transactional
	public UpdateStatusResponse updateIssueStatus(
		Long issueId,
		String code,
		UpdateStatusRequest request
	) {
		Issue issue = issueRepository.findByIdAndWorkspaceCode(issueId, code)
			.orElseThrow(IssueNotFoundException::new);

		issue.updateStatus(request.status());

		return UpdateStatusResponse.from(issue);
	}

	private Optional<Issue> findParentIssue(Long parentIssueId, String workspaceCode) {
		return Optional.ofNullable(parentIssueId)
			.map(id -> issueRepository.findByIdAndWorkspaceCode(id, workspaceCode)
				.orElseThrow(() -> new IssueNotFoundException("Issue does not exist in this workspace.")));
	}
}
