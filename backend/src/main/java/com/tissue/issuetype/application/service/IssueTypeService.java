package com.tissue.issuetype.application.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.common.util.Patchers;
import com.tissue.common.vo.Name;
import com.tissue.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.DeleteIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.RenameIssueTypeCommand;
import com.tissue.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.issuetype.application.port.in.IssueTypeUseCase;
import com.tissue.issuetype.application.port.out.IssueTypeCommandRepository;
import com.tissue.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.issuetype.application.service.validator.IssueTypeValidator;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import com.tissue.workflow.application.service.finder.WorkflowFinder;
import com.tissue.workflow.domain.Workflow;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueTypeService implements IssueTypeUseCase {

	private final ProjectFinder projectFinder;
	private final WorkflowFinder workflowFinder;
	private final IssueTypeFinder issueTypeFinder;
	private final IssueTypeCommandRepository issueTypeCommandRepository;
	private final IssueTypeValidator issueTypeValidator;

	@Override
	public IssueTypeResponse create(CreateIssueTypeCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(cmd.workflowId(), project);

		issueTypeValidator.ensureUniqueLabel(project, cmd.name());

		IssueType issueType = IssueType.create(
			project,
			cmd.name(),
			cmd.description(),
			cmd.color(),
			cmd.issueHierarchy(),
			workflow
		);

		IssueType savedType = issueTypeCommandRepository.save(issueType);

		return IssueTypeResponse.from(savedType);
	}

	@Override
	public void rename(RenameIssueTypeCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);

		if (labelUnchanged(issueType, cmd.name())) {
			return;
		}

		issueTypeValidator.ensureUniqueLabel(project, cmd.name());
		issueType.rename(cmd.name());
	}

	@Override
	public void update(PatchIssueTypeCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);

		Patchers.apply(cmd.description(), issueType::updateDescription);
		Patchers.apply(cmd.color(), issueType::updateColor);
	}

	@Override
	public void delete(DeleteIssueTypeCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.issueTypeId(), project);

		// TODO: consider IssueType migration feature(make it in IssueConfigUseCase)
		//  current policy: cant delete if there is a issue that uses this IssueType
		issueTypeValidator.ensureDeletable(issueType);

		issueType.softDelete();
	}

	private boolean labelUnchanged(IssueType it, Name newName) {
		return Objects.equals(it.getName(), newName);
	}
}
