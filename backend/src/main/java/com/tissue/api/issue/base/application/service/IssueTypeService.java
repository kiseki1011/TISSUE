package com.tissue.api.issue.base.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.base.application.dto.CreateIssueFieldCommand;
import com.tissue.api.issue.base.application.dto.CreateIssueTypeCommand;
import com.tissue.api.issue.base.application.dto.UpdateIssueTypeCommand;
import com.tissue.api.issue.base.application.finder.IssueTypeFinder;
import com.tissue.api.issue.base.domain.enums.FieldType;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldRepository;
import com.tissue.api.issue.base.infrastructure.repository.IssueRepository;
import com.tissue.api.issue.base.infrastructure.repository.IssueTypeRepository;
import com.tissue.api.issue.base.presentation.dto.response.IssueFieldResponse;
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
	private final IssueRepository issueRepository;
	private final IssueTypeRepository issueTypeRepository;
	private final IssueFieldRepository issueFieldRepository;
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

		// TODO: Should I extract the if-statement logic to a private method?
		//  Or should I encapsulate this logic in the UpdateIssueTypeCommand and simply call it here?
		//  Or should I make a update method in the IssueType entity that updates the value that are not null?
		//  I wonder what the best practice is in designing a update API
		if (cmd.label() != null) {
			issueType.updateLabel(cmd.label());
		}
		if (cmd.color() != null) {
			issueType.updateColor(cmd.color());
		}
		// if (cmd.description() != null) {
		// 	issueType.updateDescription(cmd.description());
		// }

		return IssueTypeResponse.from(issueType);
	}

	@Transactional
	public void deleteIssueType(String workspaceKey, String issueTypeKey) {
		IssueType issueType = issueTypeFinder.findIssueType(workspaceKey, issueTypeKey);

		// TODO: IssueType should not be in use. Also it should not be a system type.
		// Find if there are Issues that use the specific IssueType. If true, throw exception
		// Check if entity is a system-type. If true, throw exception
		if (issueRepository.existsByIssueType(issueType)) {
			throw new InvalidOperationException("Cannot delete if issue exists for this issue type. issueKey: '%s'"
				.formatted(issueTypeKey));
		}
		if (issueType.isSystemType()) {
			// Should I use ForbiddenOperationException?
			throw new InvalidOperationException("Cannot delete system(default) issue types. issueKey: '%s'"
				.formatted(issueTypeKey));
		}

		// TODO: Should i use soft-delete?
		issueTypeRepository.delete(issueType);
	}

	@Transactional
	public IssueFieldResponse createIssueField(CreateIssueFieldCommand cmd) {
		IssueType issueType = issueTypeFinder.findIssueType(cmd.workspaceKey(), cmd.issueTypeKey());

		// TODO: Should i move this logic(unique check) into a separate API?
		boolean labelExists = issueFieldRepository.existsByIssueTypeAndLabel(issueType, cmd.label());
		if (labelExists) {
			throw new DuplicateResourceException(
				"A field with this label already exists for the issue type: label=" + cmd.label()
					+ ", issueType label=" + issueType.getLabel());
		}

		if (cmd.fieldType() == FieldType.ENUM) {
			if (cmd.allowedOptions() == null || cmd.allowedOptions().isEmpty()) {
				throw new InvalidOperationException("ENUM fields must define at least one allowed option.");
			}
		}

		IssueField issueField = issueFieldRepository.saveAndFlush(IssueField.builder()
			.label(cmd.label())
			.description(cmd.description())
			.fieldType(cmd.fieldType())
			.required(Boolean.TRUE.equals(cmd.required()))
			.issueType(issueType)
			.allowedOptions(cmd.allowedOptions())
			.build());

		return IssueFieldResponse.from(issueField);
	}
}
