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
import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.issue.base.infrastructure.repository.EnumFieldOptionRepository;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldRepository;
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
	private final IssueTypeRepository issueTypeRepo;
	private final IssueFieldRepository issueFieldRepo;
	private final EnumFieldOptionRepository optionRepo;
	private final IssueTypeValidator issueTypeValidator;
	private final WorkflowFinder workflowFinder;
	private final IssueTypeFinder issueTypeFinder;

	@Transactional
	public IssueTypeResponse create(CreateIssueTypeCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.workflowKey());

		issueTypeValidator.ensureUniqueLabel(workspace, cmd.label());

		IssueType issueType = IssueType.create(
			workspace,
			cmd.label(),
			cmd.description(),
			cmd.color(),
			cmd.hierarchyLevel(),
			workflow
		);

		IssueType savedType = issueTypeRepo.save(issueType);

		return IssueTypeResponse.from(savedType);
	}

	@Transactional
	public IssueTypeResponse rename(RenameIssueTypeCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findIssueType(workspace, cmd.id());

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
		IssueType issueType = issueTypeFinder.findIssueType(workspace, cmd.id());

		Patchers.apply(cmd.description(), issueType::updateDescription);
		Patchers.apply(cmd.color(), issueType::updateColor);

		return IssueTypeResponse.from(issueType);
	}

	@Transactional
	public IssueTypeResponse softDelete(String workspaceKey, Long id) {
		IssueType issueType = issueTypeFinder.findIssueType(workspaceKey, id);

		issueTypeValidator.ensureDeletable(issueType);

		optionRepo.softDeleteByIssueType(issueType);
		issueFieldRepo.softDeleteByIssueType(issueType);
		issueType.softDelete();

		return IssueTypeResponse.from(issueType);
	}

	private boolean labelUnchanged(IssueType it, Label newLabel) {
		return Objects.equals(it.getLabel(), newLabel);
	}
}
