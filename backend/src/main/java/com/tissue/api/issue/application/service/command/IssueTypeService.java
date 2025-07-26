package com.tissue.api.issue.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.application.dto.CreateIssueFieldCommand;
import com.tissue.api.issue.application.dto.CreateIssueTypeCommand;
import com.tissue.api.issue.application.service.finder.IssueTypeFinder;
import com.tissue.api.issue.application.service.finder.WorkflowFinder;
import com.tissue.api.issue.domain.model.enums.FieldType;
import com.tissue.api.issue.domain.newmodel.IssueFieldDefinition;
import com.tissue.api.issue.domain.newmodel.IssueTypeDefinition;
import com.tissue.api.issue.domain.newmodel.WorkflowDefinition;
import com.tissue.api.issue.domain.util.KeyGenerator;
import com.tissue.api.issue.infrastructure.repository.IssueFieldRepository;
import com.tissue.api.issue.infrastructure.repository.IssueTypeRepository;
import com.tissue.api.issue.presentation.controller.dto.response.IssueFieldResponse;
import com.tissue.api.issue.presentation.controller.dto.response.IssueTypeResponse;
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

		IssueTypeDefinition issueType = issueTypeRepository.saveAndFlush(IssueTypeDefinition.builder()
			.workspace(workspace)
			.label(cmd.label())
			.color(cmd.color())
			.hierarchyLevel(cmd.hierarchyLevel())
			.workflow(workflow)
			.build());

		// TODO: Should i use the KeyGenerator's static method in the service(here) or encapsulate inside setKey()?
		issueType.setKey(KeyGenerator.generateIssueTypeKey(issueType.getId()));

		// TODO: Should i pass the issueType only, and set the values in IssueTypeResponse.from() like
		//  issueType.getWorkspace().getWorkspaceCode()?
		return IssueTypeResponse.from(cmd.workspaceCode(), issueType);
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

		issueField.setKey(KeyGenerator.generateFieldKey(issueField.getId()));

		// TODO: Should i just pass the issueField only, and set the values in IssueFieldResponse.from() like
		//  issueField.getIssueType().getWorkspace().getWorkspaceCode(), issueField.getIssueType().getIssueTypeKey()?
		return IssueFieldResponse.from(cmd.workspaceCode(), cmd.issueTypeKey(), issueField);
	}
}
