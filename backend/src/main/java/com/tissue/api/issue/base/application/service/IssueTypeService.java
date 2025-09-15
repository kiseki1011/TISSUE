package com.tissue.api.issue.base.application.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.issue.base.application.dto.CreateIssueTypeCommand;
import com.tissue.api.issue.base.application.dto.PatchIssueTypeCommand;
import com.tissue.api.issue.base.application.dto.RenameIssueTypeCommand;
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
	public IssueTypeResponse create(CreateIssueTypeCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.workflowKey());

		issueTypeValidator.ensureUniqueLabel(workspace, cmd.label());

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
	public IssueTypeResponse rename(RenameIssueTypeCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findIssueType(workspace, cmd.issueTypeKey());

		if (labelUnchanged(issueType, cmd.label())) {
			return IssueTypeResponse.from(issueType);
		}

		issueTypeValidator.ensureUniqueLabel(workspace, cmd.label());
		issueType.rename(cmd.label());

		return IssueTypeResponse.from(issueType);
	}

	@Transactional
	public IssueTypeResponse patch(PatchIssueTypeCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findIssueType(workspace, cmd.issueTypeKey());

		Patchers.apply(cmd.description(), issueType::updateDescription);
		Patchers.apply(cmd.color(), issueType::updateColor);

		return IssueTypeResponse.from(issueType);
	}

	@Transactional
	public void softDelete(String workspaceKey, String issueTypeKey) {
		IssueType issueType = issueTypeFinder.findIssueType(workspaceKey, issueTypeKey);

		issueTypeValidator.ensureDeletable(issueType);
		issueType.softDelete();
	}

	private boolean labelUnchanged(IssueType it, String newLabel) {
		return Objects.equals(it.getLabel(), newLabel);
	}
}
