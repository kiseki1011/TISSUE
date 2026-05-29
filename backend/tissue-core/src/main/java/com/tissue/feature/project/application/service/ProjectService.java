package com.tissue.feature.project.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.request.CreateProjectCommand;
import com.tissue.feature.project.application.dto.request.UpdateProjectCommand;
import com.tissue.feature.project.application.dto.response.ProjectResponse;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.port.usecase.ProjectUseCase;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.application.service.validator.ProjectValidator;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.support.util.Patchers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService implements ProjectUseCase {

    private final MemberFinder memberFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final ProjectFinder projectFinder;
    private final ProjectValidator projectValidator;
    private final ProjectCommandRepository projectRepository;
    private final ProjectMemberCommandRepository projectMemberRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Override
    public ProjectResponse create(CreateProjectCommand cmd, Long actorMemberId) {
        Member actor = memberFinder.getActiveById(actorMemberId);

        projectValidator.ensureUniqueProjectKey(cmd.projectKey());

        Project project = Project.create(cmd.projectKey(), cmd.title(), cmd.description());
        projectRepository.save(project);

        ProjectMember projectCreator = ProjectMember.createManager(project, actor);
        projectMemberRepository.save(projectCreator);

        return ProjectResponse.from(project);
    }

    @Override
    public void update(ProjectIdentifier pid, UpdateProjectCommand cmd, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getWithProject(pid.projectKey(), actorMemberId);

        Project project = projectFinder.getByProjectKey(pid.projectKey());

        projectAuthorizationService.requireProjectManager(actor);

        Patchers.apply(cmd.title(), project::updateTitle);
        Patchers.apply(cmd.description(), project::updateDescription);
        Patchers.apply(cmd.projectVisibility(), project::updateVisibility);
    }

    @Override
    public void delete(ProjectIdentifier pid, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getWithProject(pid.projectKey(), actorMemberId);

        Project project = actor.getProject();

        projectAuthorizationService.requireSystemAdmin(actor);

        project.softDelete();
    }

    @Override
    public void archive(ProjectIdentifier pid, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getWithProject(pid.projectKey(), actorMemberId);

        projectAuthorizationService.requireProjectManager(actor);

        Project project = projectFinder.getByProjectKey(pid.projectKey());
        project.archive();
    }

    @Override
    public void restoreArchived(ProjectIdentifier pid, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getWithProject(pid.projectKey(), actorMemberId);

        projectAuthorizationService.requireProjectManager(actor);

        Project project = projectFinder.getByProjectKey(pid.projectKey());
        project.restoreArchived();
    }

    @Override
    public void restoreDeleted(ProjectIdentifier pid, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getWithProject(pid.projectKey(), actorMemberId);

        projectAuthorizationService.requireProjectManager(actor);

        Project project = projectFinder.getDeletedByProjectKey(pid.projectKey());
        project.restoreSoftDeleted();
    }
}
