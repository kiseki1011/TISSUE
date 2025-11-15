package com.tissue.api.issuetype.application.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issuetype.application.dto.CreateIssueTypeCommand;
import com.tissue.api.issuetype.application.dto.PatchIssueTypeCommand;
import com.tissue.api.issuetype.application.dto.RenameIssueTypeCommand;
import com.tissue.api.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.issuetype.domain.service.validator.IssueTypeValidator;
import com.tissue.api.issuetype.presentation.dto.response.IssueTypeResponse;
import com.tissue.api.issuetype.repository.EnumFieldOptionCommandRepository;
import com.tissue.api.issuetype.repository.IssueFieldCommandRepository;
import com.tissue.api.issuetype.repository.IssueTypeQueryRepository;
import com.tissue.api.workflow.application.finder.WorkflowFinder;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueTypeService {

	private final WorkspaceFinder workspaceFinder;
	private final WorkflowFinder workflowFinder;
	private final IssueTypeFinder typeFinder;

	private final IssueTypeQueryRepository typeQueryRepo;
	private final IssueFieldCommandRepository fieldCommandRepo;
	private final EnumFieldOptionCommandRepository fieldOptionCommandRepo;

	private final IssueTypeValidator typeValidator;

	@Transactional
	public IssueTypeResponse create(CreateIssueTypeCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.workflowId());

		typeValidator.ensureUniqueLabel(workspace, cmd.label());

		IssueType issueType = IssueType.create(
			workspace,
			cmd.label(),
			cmd.description(),
			cmd.color(),
			cmd.issueHierarchy(),
			workflow
		);

		IssueType savedType = typeQueryRepo.save(issueType);

		return IssueTypeResponse.from(savedType);
	}

	@Transactional
	public IssueTypeResponse rename(RenameIssueTypeCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		IssueType issueType = typeFinder.findByIdAndWorkspace(cmd.id(), workspace);

		if (labelUnchanged(issueType, cmd.label())) {
			return IssueTypeResponse.from(issueType);
		}

		typeValidator.ensureUniqueLabel(workspace, cmd.label());
		issueType.rename(cmd.label());

		return IssueTypeResponse.from(issueType);
	}

	@Transactional
	public IssueTypeResponse patch(PatchIssueTypeCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		IssueType issueType = typeFinder.findByIdAndWorkspace(cmd.id(), workspace);

		Patchers.apply(cmd.description(), issueType::updateDescription);
		Patchers.apply(cmd.color(), issueType::updateColor);

		return IssueTypeResponse.from(issueType);
	}

	@Transactional
	public IssueTypeResponse softDelete(String workspaceKey, Long id) {
		Workspace workspace = workspaceFinder.findWorkspace(workspaceKey);
		IssueType issueType = typeFinder.findByIdAndWorkspace(id, workspace);

		typeValidator.ensureDeletable(issueType);

		fieldOptionCommandRepo.softDeleteByIssueType(issueType);
		fieldCommandRepo.softDeleteByIssueType(issueType);
		issueType.softDelete();

		return IssueTypeResponse.from(issueType);
	}

	private boolean labelUnchanged(IssueType it, Label newLabel) {
		return Objects.equals(it.getLabel(), newLabel);
	}
}
