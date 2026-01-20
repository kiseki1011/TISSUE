package com.tissue.project.application.service;

import com.tissue.common.enums.JoinMethod;
import com.tissue.global.exception.base.BadRequestException;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.dto.request.AddProjectMembersCommand;
import com.tissue.project.application.dto.request.ChangeProjectRoleCommand;
import com.tissue.project.application.dto.request.DirectJoinProjectCommand;
import com.tissue.project.application.dto.request.KickProjectMemberCommand;
import com.tissue.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.project.application.port.in.ProjectMemberUseCase;
import com.tissue.project.application.port.out.ProjectMemberCommandRepository;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.event.ProjectEventPublisher;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.application.service.validator.ProjectValidator;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.exception.ProjectErrorCode;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final ProjectValidator projectValidator;
    private final ProjectAuthorizationService projectAuthService;
    private final ProjectEventPublisher eventPublisher;

    @Override
    public ProjectMembersCommandResult addMembers(AddProjectMembersCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        projectAuthService.requireProjectAdmin(actorContext);

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());
        Project project = projectFinder.getModifiableBy(actorContext.projectId());

        Set<Long> targetMemberIds = cmd.extractMemberIds();
        Map<Long, ProjectRole> roleMap = cmd.extractRoleMap();

        List<WorkspaceMember> workspaceMembers =
                workspaceMemberFinder.getAllBy(targetMemberIds, actorContext.workspaceKey());

        Set<Long> existingMemberIds = projectMemberFinder.getExistingMemberIdsBy(project, targetMemberIds);

        List<ProjectMember> newMembers = new ArrayList<>();

        for (WorkspaceMember wm : workspaceMembers) {
            if (existingMemberIds.contains(wm.getMemberId())) {
                continue;
            }

            // TODO: 개선 - 꼭 Objects.requireNonNull를 사용해야 할까?
            ProjectRole role = Objects.requireNonNull(roleMap.get(wm.getMemberId()));
            newMembers.add(ProjectMember.create(project, wm, role));
        }

        projectMemberRepository.saveAll(newMembers);

        newMembers.forEach(pm -> eventPublisher.publishMemberJoinedProject(
                pm, workspace, JoinMethod.BY_ADMIN, actorContext.memberId(), actorContext.displayName()));

        return ProjectMembersCommandResult.of(project, newMembers);
    }

    @Override
    public ProjectMemberCommandResult joinViaDirect(DirectJoinProjectCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), actorContext.workspaceKey());

        projectAuthService.requireDirectJoinPermission(actorContext, project);
        projectValidator.ensureNotAlreadyJoined(project, actorContext.memberId());

        WorkspaceMember actor = workspaceMemberFinder.getActive(actorContext.workspaceMemberId());

        ProjectMember projectMember = ProjectMember.create(project, actor, project.getDefaultJoinRole());
        projectMemberRepository.save(projectMember);

        eventPublisher.publishMemberJoinedProject(
                projectMember, workspace, JoinMethod.DIRECT, actorContext.memberId(), actorContext.displayName());

        return ProjectMemberCommandResult.of(projectMember);
    }

    @Override
    public void leave(ProjectMemberContext actorContext) {
        projectAuthService.requireProjectViewer(actorContext);

        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        ProjectMember actor = projectMemberFinder.getActive(project, actorContext.memberId());

        actor.remove();

        // TODO: ProjectMemberLeftEvent
    }

    @Override
    public void kickMember(KickProjectMemberCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        projectAuthService.requireProjectAdmin(actorContext);

        if (actorContext.memberId().equals(cmd.targetMemberId())) {
            throw new BadRequestException(ProjectErrorCode.SELF_KICK_NOT_ALLOWED);
        }

        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        ProjectMember target = projectMemberFinder.getActive(project, cmd.targetMemberId());

        target.remove();

        // TODO: ProjectMemberKickedEvent
    }

    @Override
    public void changeProjectRole(ChangeProjectRoleCommand cmd) {
        ProjectMemberContext actor = cmd.actor();
        Project project = projectFinder.getModifiableBy(actor.projectId());
        ProjectMember target = projectMemberFinder.getActive(project, cmd.targetMemberId());

        if (actor.memberId().equals(cmd.targetMemberId())) {
            throw new BadRequestException(ProjectErrorCode.SELF_ROLE_MODIFICATION_NOT_ALLOWED);
        }

        projectAuthService.requireRoleGrantPermission(actor, target, cmd.grantRole());

        ProjectRole oldRole = target.getRole();
        target.changeRole(cmd.grantRole());

        eventPublisher.publishProjectRoleChanged(target, oldRole, cmd.grantRole(), actor);
    }
}
