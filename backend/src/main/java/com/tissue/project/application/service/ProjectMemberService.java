package com.tissue.project.application.service;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.project.application.port.in.ProjectMemberUseCase;
import com.tissue.project.application.port.out.ProjectMemberCommandRepository;
import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.ProjectRole;
import com.tissue.project.domain.exception.ProjectArchivedException;
import com.tissue.project.domain.exception.ProjectErrorCode;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.WorkspaceMember;
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
            String projectKey, Set<Long> targetMemberIds, WorkspaceMemberContext actorContext) {
        Project project = projectFinder.getBy(projectKey, actorContext.workspaceKey());

        projectAuthService.requireProjectEditPermission(actorContext, project);

        List<WorkspaceMember> workspaceMembers =
                workspaceMemberFinder.getAllBy(actorContext.workspaceKey(), targetMemberIds);

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
    public ProjectMemberCommandResult join(String projectKey, WorkspaceMemberContext actorContext) {
        Project project = projectFinder.getBy(actorContext.workspaceKey(), projectKey);

        projectAuthService.requireJoinPermission(actorContext, project);

        if (projectMemberQueryRepository.existsByProjectAndMemberId(project, actorContext.memberId())) {
            return new ProjectMemberCommandResult(actorContext.workspaceKey(), projectKey, actorContext.memberId());
        }

        WorkspaceMember actorWorkspaceMember =
                workspaceMemberFinder.getBy(actorContext.workspaceKey(), actorContext.memberId());

        ProjectMember projectMember = ProjectMember.create(project, actorWorkspaceMember);
        projectMemberRepository.save(projectMember);

        return ProjectMemberCommandResult.of(projectMember);
    }

    @Override
    public void changeRole(
            String projectKey, Long targetMemberId, ProjectRole role, WorkspaceMemberContext actorContext) {
        ProjectMember target =
                projectMemberFinder.getWithProjectBy(actorContext.workspaceKey(), projectKey, targetMemberId);

        projectAuthService.requireProjectEditPermission(actorContext, target.getProject());

        target.changeRole(role);
    }

    @Override
    public void leave(String projectKey, WorkspaceMemberContext actorContext) {
        String workspaceKey = actorContext.workspaceKey();

        ProjectMember actor = projectMemberFinder.getWithProjectBy(workspaceKey, projectKey, actorContext.memberId());
        ensureProjectModifiable(actor, workspaceKey, projectKey);

        // TODO: actor.remove(); -> projectMemberRepository.delete(actor)
        //        actor.remove();
    }

    @Override
    public void kickMember(String projectKey, Long targetMemberId, WorkspaceMemberContext actorContext) {
        if (Objects.equals(actorContext.memberId(), targetMemberId)) {
            throw new BadRequestException(ProjectErrorCode.SELF_KICK_NOT_ALLOWED);
        }

        ProjectMember target = projectMemberFinder.getWithProjectBy(actorContext.workspaceKey(), projectKey, targetMemberId);
        ensureProjectModifiable(target, actorContext.workspaceKey(), projectKey);

        projectAuthService.requireProjectEditPermission(actorContext, target.getProject());

        // TODO: target.remove(); -> projectMemberRepository.delete(target)
        //        target.remove();
    }

    private void ensureProjectModifiable(ProjectMember actor, String workspaceKey, String projectKey) {
        if (actor.getProject().isArchived()) {
            throw new ProjectArchivedException(workspaceKey, projectKey);
        }
    }
}
