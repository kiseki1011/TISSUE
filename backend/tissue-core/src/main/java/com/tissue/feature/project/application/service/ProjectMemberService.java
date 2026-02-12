package com.tissue.feature.project.application.service;

import com.tissue.feature.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.feature.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.application.port.usecase.ProjectMemberUseCase;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
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
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final ProjectAuthorizationService projectAuthService;

    @Override
    public ProjectMembersCommandResult addMembers(
            ProjectIdentifier projectIdentifier, Set<Long> targetMemberIds, Long memberId) {

        Project project = projectFinder.getBy(projectIdentifier.projectKey(), projectIdentifier.workspaceKey());

        WorkspaceMember actor = workspaceMemberFinder.getBy(projectIdentifier.workspaceKey(), memberId);
        projectAuthService.requireProjectEditPermission(actor, project);

        List<WorkspaceMember> workspaceMembers =
                workspaceMemberFinder.getAllBy(projectIdentifier.workspaceKey(), targetMemberIds);

        Set<Long> existingMemberIds = projectMemberFinder.getExistingMemberIdsBy(project, targetMemberIds);

        List<ProjectMember> newMembers = new ArrayList<>();

        // TODO: 최적화 고려
        for (WorkspaceMember wm : workspaceMembers) {
            if (existingMemberIds.contains(wm.getMemberId())) {
                continue;
            }
            newMembers.add(ProjectMember.create(project, wm));
        }

        projectMemberRepository.saveAll(newMembers);

        return ProjectMembersCommandResult.of(project, newMembers);
    }

    @Override
    public ProjectMemberCommandResult join(ProjectIdentifier projectIdentifier, Long memberId) {
        Project project = projectFinder.getBy(projectIdentifier.workspaceKey(), projectIdentifier.projectKey());

        WorkspaceMember actor = workspaceMemberFinder.getBy(projectIdentifier.workspaceKey(), memberId);
        projectAuthService.requireJoinPermission(actor, project);

        if (projectMemberQueryRepository.existsByProjectAndMemberId(project, memberId)) {
            return new ProjectMemberCommandResult(
                    projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), memberId);
        }

        ProjectMember projectMember = ProjectMember.create(project, actor);
        projectMemberRepository.save(projectMember);

        return ProjectMemberCommandResult.of(projectMember);
    }

    @Override
    public void changeRole(ProjectIdentifier projectIdentifier, Long targetMemberId, ProjectRole role, Long memberId) {

        ProjectMember target = projectMemberFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), targetMemberId);

        WorkspaceMember actor = workspaceMemberFinder.getBy(projectIdentifier.workspaceKey(), memberId);
        projectAuthService.requireProjectEditPermission(actor, target.getProject());

        target.changeRole(role);
    }

    @Override
    public void leave(ProjectIdentifier projectIdentifier, Long memberId) {
        String workspaceKey = projectIdentifier.workspaceKey();
        String projectKey = projectIdentifier.projectKey();

        ProjectMember actor = projectMemberFinder.getWithProjectBy(workspaceKey, projectKey, memberId);
        ensureProjectModifiable(actor, workspaceKey, projectKey);

        // TODO: actor.remove(); -> projectMemberRepository.delete(actor)
        //        actor.remove();
    }

    @Override
    public void kickMember(ProjectIdentifier projectIdentifier, Long targetMemberId, Long memberId) {
        if (Objects.equals(memberId, targetMemberId)) {
            throw new BadRequestException(ProjectErrorCode.SELF_KICK_NOT_ALLOWED);
        }

        ProjectMember target = projectMemberFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), targetMemberId);
        ensureProjectModifiable(target, projectIdentifier.workspaceKey(), projectIdentifier.projectKey());

        WorkspaceMember actor = workspaceMemberFinder.getBy(projectIdentifier.workspaceKey(), memberId);
        projectAuthService.requireProjectEditPermission(actor, target.getProject());

        // TODO: target.remove(); -> projectMemberRepository.delete(target)
        //        target.remove();
    }

    private void ensureProjectModifiable(ProjectMember actor, String workspaceKey, String projectKey) {
        if (actor.getProject().isArchived()) {
            throw new ProjectArchivedException(workspaceKey, projectKey);
        }
    }
}
