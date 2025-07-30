package com.tissue.api.issue.base.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.workflow.application.finder.WorkflowFinder;
import com.tissue.api.issue.base.application.dto.CreateIssueFieldCommand;
import com.tissue.api.issue.base.application.dto.CreateIssueTypeCommand;
import com.tissue.api.issue.base.application.finder.IssueTypeFinder;
import com.tissue.api.issue.base.domain.enums.FieldType;
import com.tissue.api.issue.base.domain.model.IssueFieldDefinition;
import com.tissue.api.issue.base.domain.model.IssueTypeDefinition;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldRepository;
import com.tissue.api.issue.base.infrastructure.repository.IssueTypeRepository;
import com.tissue.api.issue.workflow.domain.model.WorkflowDefinition;
import com.tissue.api.issue.base.presentation.dto.response.IssueFieldResponse;
import com.tissue.api.issue.base.presentation.dto.response.IssueTypeResponse;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueTypeService {

	private final WorkspaceFinder workspaceFinder;
	private final IssueTypeRepository issueTypeRepository;
	private final IssueFieldRepository issueFieldRepository;
	private final WorkflowFinder workflowFinder;
	private final IssueTypeFinder issueTypeFinder;

	@Transactional
	public IssueTypeResponse createIssueType(CreateIssueTypeCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceCode());
		WorkflowDefinition workflow = workflowFinder.findWorkflow(cmd.workspaceCode(), cmd.workflowKey());

		IssueTypeDefinition issueType = issueTypeRepository.save(IssueTypeDefinition.builder()
			.workspace(workspace)
			.label(cmd.label())
			.color(cmd.color())
			.hierarchyLevel(cmd.hierarchyLevel())
			.workflow(workflow)
			.build());

		return IssueTypeResponse.from(issueType);
	}

	@Transactional
	public IssueFieldResponse createIssueField(CreateIssueFieldCommand cmd) {
		IssueTypeDefinition issueType = issueTypeFinder.findIssueType(cmd.workspaceCode(), cmd.issueTypeKey());

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

		// TODO: Why use Boolean.TRUE.equals?
		IssueFieldDefinition issueField = issueFieldRepository.saveAndFlush(IssueFieldDefinition.builder()
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
