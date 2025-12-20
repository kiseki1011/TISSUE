package com.tissue.issuetype.application.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.tissue.common.util.Patchers;
import com.tissue.common.vo.Label;
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
@RequiredArgsConstructor
public class IssueTypeService implements IssueTypeUseCase {

	private final ProjectFinder projectFinder;
	private final WorkflowFinder workflowFinder;
	private final IssueTypeFinder issueTypeFinder;
	private final IssueTypeCommandRepository issueTypeCommandRepository;
	private final IssueTypeValidator issueTypeValidator;

	public IssueTypeResponse create(CreateIssueTypeCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(cmd.workflowId(), project);

		issueTypeValidator.ensureUniqueLabel(project, cmd.label());

		IssueType issueType = IssueType.create(
			project,
			cmd.label(),
			cmd.description(),
			cmd.color(),
			cmd.issueHierarchy(),
			workflow
		);

		IssueType savedType = issueTypeCommandRepository.save(issueType);

		return IssueTypeResponse.from(savedType);
	}

	public void rename(RenameIssueTypeCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.id(), project);

		if (labelUnchanged(issueType, cmd.label())) {
			return;
		}

		issueTypeValidator.ensureUniqueLabel(project, cmd.label());
		issueType.rename(cmd.label());
	}

	public void update(PatchIssueTypeCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.id(), project);

		Patchers.apply(cmd.description(), issueType::updateDescription);
		Patchers.apply(cmd.color(), issueType::updateColor);
	}

	// TODO: soft-delete 사용
	public void delete(DeleteIssueTypeCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.id(), project);

		// TODO: 해당 IssueType를 사용한 이슈가 WorkflowState의 StateCategory가 DONE이 아닌게 존재한다면 삭제 불가
		issueTypeValidator.ensureDeletable(issueType);

		issueType.softDelete();
	}

	private boolean labelUnchanged(IssueType it, Label newLabel) {
		return Objects.equals(it.getLabel(), newLabel);
	}
}
