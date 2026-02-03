package com.tissue.project.application.service;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.dto.request.AddProjectMembersCommand;
import com.tissue.project.application.dto.request.DirectJoinProjectCommand;
import com.tissue.project.application.dto.request.KickProjectMemberCommand;
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
import com.tissue.project.domain.exception.ProjectErrorCode;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.WorkspaceMember;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectMemberService implements ProjectMemberUseCase {

    private final WorkspaceFinder workspaceFinder;
    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final ProjectMemberCommandRepository projectMemberRepository;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final ProjectAuthorizationService projectAuthService;

    @Override
    public ProjectMembersCommandResult addMembers(AddProjectMembersCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        // TODO: requireWorkspaceAdmin or requireProjectCreator -> requireProjectEditPermission

        workspaceFinder.getModifiableBy(actorContext.workspaceId());
        Project project = projectFinder.getModifiableBy(actorContext.projectId());

        Set<Long> targetMemberIds = cmd.targetMemberIds();

        List<WorkspaceMember> workspaceMembers =
                workspaceMemberFinder.getAllBy(targetMemberIds, actorContext.workspaceKey());

        Set<Long> existingMemberIds = projectMemberFinder.getExistingMemberIdsBy(project, targetMemberIds);

        List<ProjectMember> newMembers = new ArrayList<>();

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
    public ProjectMemberCommandResult join(DirectJoinProjectCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), actorContext.workspaceKey());

        projectAuthService.requireJoinPermission(actorContext, project);

        if (projectMemberQueryRepository.existsByProjectAndMemberId(project, actorContext.memberId())) {
            return new ProjectMemberCommandResult(
                    actorContext.workspaceKey(), cmd.projectKey(), actorContext.memberId());
        }

        WorkspaceMember actor = workspaceMemberFinder.getActive(actorContext.workspaceMemberId());

        ProjectMember projectMember = ProjectMember.create(project, actor);
        projectMemberRepository.save(projectMember);

        return ProjectMemberCommandResult.of(projectMember);
    }

    @Override
    public void leave(ProjectMemberContext actorContext) {
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        ProjectMember actor = projectMemberFinder.getActive(project, actorContext.memberId());

        actor.remove();
    }

    @Override
    public void kickMember(KickProjectMemberCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        // TODO: requireWorkspaceAdmin or requireProjectCreator -> requireProjectEditPermission

        if (actorContext.memberId().equals(cmd.targetMemberId())) {
            throw new BadRequestException(ProjectErrorCode.SELF_KICK_NOT_ALLOWED);
        }

        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        ProjectMember target = projectMemberFinder.getActive(project, cmd.targetMemberId());

        target.remove();
    }
}
