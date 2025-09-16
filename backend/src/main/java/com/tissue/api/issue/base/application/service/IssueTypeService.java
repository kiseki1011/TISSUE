package com.tissue.api.issue.base.application.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.issue.base.application.dto.CreateIssueTypeCommand;
import com.tissue.api.issue.base.application.dto.PatchIssueTypeCommand;
import com.tissue.api.issue.base.application.dto.RenameIssueTypeCommand;
import com.tissue.api.issue.base.application.finder.IssueFieldFinder;
import com.tissue.api.issue.base.application.finder.IssueTypeFinder;
import com.tissue.api.issue.base.application.validator.IssueTypeValidator;
import com.tissue.api.issue.base.domain.model.EnumFieldOption;
import com.tissue.api.issue.base.domain.model.EnumFieldOptions;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.issue.base.infrastructure.repository.EnumFieldOptionRepository;
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
	private final EnumFieldOptionRepository optionRepo;
	private final IssueTypeValidator issueTypeValidator;
	private final WorkflowFinder workflowFinder;
	private final IssueTypeFinder issueTypeFinder;
	private final IssueFieldFinder issueFieldFinder;

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

		IssueType savedType = issueTypeRepository.save(issueType);

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
	public void softDelete(String workspaceKey, Long id) {
		IssueType issueType = issueTypeFinder.findIssueType(workspaceKey, id);

		issueTypeValidator.ensureDeletable(issueType);

		// TODO: 계단식으로 전파되는 soft-delete의 구현을 더 효율적으로 못하나?
		//  현재의 nested for 문이 과연 최선일까 고민이 됨.
		//  IssueType -> List<IssueField> -> List<EnumFieldOption>에 많아봤자 100개가 안 넘어갈 것 같은데.
		List<IssueField> fields = issueFieldFinder.findByIssueType(issueType);

		for (IssueField field : fields) {
			EnumFieldOptions options = EnumFieldOptions.fromActiveOrdered(field, findActiveOptions(field));
			options.softDeleteAll();
			field.softDelete();
		}

		issueType.softDelete();
	}

	private boolean labelUnchanged(IssueType it, String newLabel) {
		return Objects.equals(it.getLabel(), newLabel);
	}

	private List<EnumFieldOption> findActiveOptions(IssueField field) {
		return optionRepo.findByFieldOrderByPositionAsc(field);
	}
}
