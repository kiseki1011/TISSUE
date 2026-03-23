package com.tissue.feature.project.application.service;

import com.tissue.feature.project.application.dto.response.ProjectMemberResponse;
import com.tissue.feature.project.application.dto.response.ProjectMembersResponse;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.port.usecase.ProjectMemberUseCase;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectMemberService implements ProjectMemberUseCase {

    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final ProjectMemberCommandRepository projectMemberRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    @Override
    public ProjectMembersResponse addMembers(ProjectIdentifier pid, Set<Long> targetMemberIds, Long actorMemberId) {

        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        Project project = projectFinder.getBy(pid.workspaceKey(), pid.projectKey());

        projectAuthorizationService.requireProjectManager(actor);

        List<WorkspaceMember> workspaceMembers =
                workspaceMemberFinder.getAllIncludingSoftDeleted(pid.workspaceKey(), targetMemberIds);

        Set<Long> existingMemberIds = projectMemberFinder.getExistingMemberIds(project, targetMemberIds);

        List<ProjectMember> newMembers = new ArrayList<>();

        for (WorkspaceMember wm : workspaceMembers) {
            if (existingMemberIds.contains(wm.getMemberId())) {
                continue;
            }
            newMembers.add(ProjectMember.create(project, wm));
        }

        projectMemberRepository.saveAll(newMembers);

        return ProjectMembersResponse.of(project, newMembers);
    }

    @Override
    public ProjectMemberResponse join(ProjectIdentifier pid, Long actorMemberId) {
        Project project = projectFinder.getBy(pid.workspaceKey(), pid.projectKey());

        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(pid.workspaceKey(), actorMemberId);
        projectAuthorizationService.requireJoinPermission(actor, project);

        if (projectMemberFinder.existsByIncludingSoftDeleted(project, actorMemberId)) {
            return new ProjectMemberResponse(pid.workspaceKey(), pid.projectKey(), actorMemberId);
        }

        ProjectMember projectMember = ProjectMember.create(project, actor);
        projectMemberRepository.save(projectMember);

        return ProjectMemberResponse.of(projectMember);
    }

    @Override
    public void changeRole(ProjectIdentifier pid, Long targetMemberId, ProjectRole role, Long actorMemberId) {

        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        ProjectMember target = projectMemberFinder.getWithProject(pid.workspaceKey(), pid.projectKey(), targetMemberId);

        projectAuthorizationService.requireHigherRole(actor, target);

        target.changeRole(role);
    }

    @Override
    public void leave(ProjectIdentifier pid, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getWithProject(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        actor.softDelete();
    }

    @Override
    public void kickMember(ProjectIdentifier pid, Long targetMemberId, Long actorMemberId) {
        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        if (Objects.equals(actorMemberId, targetMemberId)) {
            throw new BadRequestException(ProjectErrorCode.SELF_KICK_NOT_ALLOWED);
        }

        ProjectMember target = projectMemberFinder.getWithProject(pid.workspaceKey(), pid.projectKey(), targetMemberId);

        projectAuthorizationService.requireHigherRole(actor, target);

        target.softDelete();
    }
}
