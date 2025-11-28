package com.tissue.api.issuetype.application.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issuetype.application.dto.CreateIssueTypeCommand;
import com.tissue.api.issuetype.application.dto.DeleteIssueTypeCommand;
import com.tissue.api.issuetype.application.dto.PatchIssueTypeCommand;
import com.tissue.api.issuetype.application.dto.RenameIssueTypeCommand;
import com.tissue.api.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.issuetype.domain.service.validator.IssueTypeValidator;
import com.tissue.api.issuetype.presentation.dto.response.IssueTypeResponse;
import com.tissue.api.issuetype.repository.EnumFieldOptionCommandRepository;
import com.tissue.api.issuetype.repository.IssueFieldCommandRepository;
import com.tissue.api.issuetype.repository.IssueTypeQueryRepository;
import com.tissue.api.project.application.service.finder.ProjectFinder;
import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.application.finder.WorkflowFinder;
import com.tissue.api.workflow.domain.Workflow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueTypeService {

	private final ProjectFinder projectFinder;
	private final WorkflowFinder workflowFinder;
	private final IssueTypeFinder issueTypeFinder;

	private final IssueTypeQueryRepository issueTypeQueryRepo;
	private final IssueFieldCommandRepository issueFieldCommandRepo;
	private final EnumFieldOptionCommandRepository fieldOptionCommandRepo;

	private final IssueTypeValidator issyeTypeValidator;

	@Transactional
	public IssueTypeResponse create(CreateIssueTypeCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(project, cmd.workflowId());

		issyeTypeValidator.ensureUniqueLabel(project, cmd.label());

		IssueType issueType = IssueType.create(
			project,
			cmd.label(),
			cmd.description(),
			cmd.color(),
			cmd.issueHierarchy(),
			workflow
		);

		IssueType savedType = issueTypeQueryRepo.save(issueType);

		return IssueTypeResponse.from(savedType);
	}

	@Transactional
	public IssueTypeResponse rename(RenameIssueTypeCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.id(), project);

		if (labelUnchanged(issueType, cmd.label())) {
			return IssueTypeResponse.from(issueType);
		}

		issyeTypeValidator.ensureUniqueLabel(project, cmd.label());
		issueType.rename(cmd.label());

		return IssueTypeResponse.from(issueType);
	}

	@Transactional
	public IssueTypeResponse update(PatchIssueTypeCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.id(), project);

		Patchers.apply(cmd.description(), issueType::updateDescription);
		Patchers.apply(cmd.color(), issueType::updateColor);

		return IssueTypeResponse.from(issueType);
	}

	// TODO: hard-delete 사용
	@Transactional
	public IssueTypeResponse delete(DeleteIssueTypeCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		IssueType issueType = issueTypeFinder.findBy(cmd.id(), project);

		// TODO: 해당 IssueType를 사용한 이슈가 단 하나라도 존재하면 삭제 불가
		issyeTypeValidator.ensureDeletable(issueType);

		fieldOptionCommandRepo.softDeleteByIssueType(issueType);
		issueFieldCommandRepo.softDeleteByIssueType(issueType);
		issueType.softDelete();

		return IssueTypeResponse.from(issueType);
	}

	private boolean labelUnchanged(IssueType it, Label newLabel) {
		return Objects.equals(it.getLabel(), newLabel);
	}
}
