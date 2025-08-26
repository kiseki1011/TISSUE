package com.tissue.api.issue.base.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.base.application.dto.CreateIssueTypeCommand;
import com.tissue.api.issue.base.application.dto.UpdateIssueTypeCommand;
import com.tissue.api.issue.base.application.finder.IssueTypeFinder;
import com.tissue.api.issue.base.application.validator.IssueTypeValidator;
import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.issue.base.infrastructure.repository.IssueTypeRepository;
import com.tissue.api.issue.base.presentation.dto.response.IssueTypeResponse;
import com.tissue.api.issue.workflow.application.finder.WorkflowFinder;
import com.tissue.api.issue.workflow.domain.model.Workflow;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueTypeService {

	private final WorkspaceFinder workspaceFinder;
	private final IssueTypeRepository issueTypeRepository;
	private final IssueTypeValidator issueTypeValidator;
	private final WorkflowFinder workflowFinder;
	private final IssueTypeFinder issueTypeFinder;

	@Transactional
	public IssueTypeResponse createIssueType(CreateIssueTypeCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		Workflow workflow = workflowFinder.findWorkflow(cmd.workspaceKey(), cmd.workflowKey());

		IssueType issueType = issueTypeRepository.save(IssueType.builder()
			.workspace(workspace)
			.label(cmd.label())
			.description(cmd.description())
			.color(cmd.color())
			.hierarchyLevel(cmd.hierarchyLevel())
			.workflow(workflow)
			.build());

		return IssueTypeResponse.from(issueType);
	}

	@Transactional
	public IssueTypeResponse updateIssueType(UpdateIssueTypeCommand cmd) {
		IssueType issueType = issueTypeFinder.findIssueType(cmd.workspaceKey(), cmd.issueTypeKey());
		issueType.updateMetaData(cmd.label(), cmd.description(), cmd.color());
		return IssueTypeResponse.from(issueType);
	}

	@Transactional
	public void deleteIssueType(String workspaceKey, String issueTypeKey) {
		IssueType issueType = issueTypeFinder.findIssueType(workspaceKey, issueTypeKey);
		issueTypeValidator.ensureDeletable(issueType);
		issueTypeRepository.delete(issueType);
	}
}
