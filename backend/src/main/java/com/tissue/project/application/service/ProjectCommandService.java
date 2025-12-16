package com.tissue.project.application.service;

import org.springframework.stereotype.Service;

import com.tissue.common.util.Patchers;
import com.tissue.project.application.dto.request.CreateProjectCommand;
import com.tissue.project.application.dto.request.DeleteProjectCommand;
import com.tissue.project.application.dto.request.UpdateProjectCommand;
import com.tissue.project.application.dto.response.ProjectCommandResult;
import com.tissue.project.application.port.in.ProjectCommandUseCase;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.application.port.out.ProjectCommandRepository;
import com.tissue.project.application.service.validator.ProjectValidator;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.domain.Workspace;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectCommandService implements ProjectCommandUseCase {

	private final WorkspaceFinder workspaceFinder;
	private final ProjectFinder projectFinder;
	private final ProjectValidator projectValidator;
	private final ProjectCommandRepository projectRepository;

	@Override
	public ProjectCommandResult create(CreateProjectCommand cmd) {

		Workspace workspace = workspaceFinder.findByKey(cmd.workspaceKey());

		Project project = Project.create(
			workspace,
			cmd.projectKey(),
			cmd.title(),
			cmd.description()
		);

		projectValidator.ensureUniqueProjectKey(project.getKey(), workspace.getKey());

		projectRepository.save(project);

		// TODO: ProjectCreatedEvent

		return ProjectCommandResult.from(project);
	}

	@Override
	public ProjectCommandResult update(UpdateProjectCommand cmd) {

		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());

		Patchers.apply(cmd.title(), project::updateTitle);
		Patchers.apply(cmd.description(), project::updateDescription);
		Patchers.apply(cmd.projectVisibility(), project::updateVisibility);
		Patchers.apply(cmd.defaultJoinRole(), project::updateDefaultJoinRole);

		// TODO: ProjectInfoUpdatedEvent

		return ProjectCommandResult.from(project);
	}

	@Override
	public ProjectCommandResult delete(DeleteProjectCommand cmd) {

		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());

		// TODO: Project soft-delete 시 안의 내용물(스프린트, 이슈, 프로젝트 멤버, 등)은 어떻게 처리해야할까?
		//  - 다 같이 cascade로 soft-delete 처리? 너무 복잡한데? 다시 restore 하는 로직도 복잡할 것 같고.
		project.softDelete();

		return ProjectCommandResult.from(project);
	}
}
