package com.tissue.project.application.service;

import com.tissue.common.enums.JoinMethod;
import com.tissue.global.exception.base.BadRequestException;
import com.tissue.project.application.dto.request.AddProjectMembersCommand;
import com.tissue.project.application.dto.request.ChangeProjectRoleCommand;
import com.tissue.project.application.dto.request.DirectJoinProjectCommand;
import com.tissue.project.application.dto.request.KickProjectMemberCommand;
import com.tissue.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.project.application.port.in.ProjectParticipationUseCase;
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
import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;
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
public class ProjectParticipationService implements ProjectParticipationUseCase {

    private final WorkspaceFinder workspaceFinder;
    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final ProjectValidator projectValidator;
    private final ProjectMemberCommandRepository projectMemberRepository;
    private final ProjectAuthorizationService projectAuthService;
    private final ProjectEventPublisher eventPublisher;

    @Override
    public ProjectMembersCommandResult addMembers(AddProjectMembersCommand cmd) {
        WorkspaceMemberInfo actor = cmd.actor();
        projectAuthService.requireProjectAdmin(cmd.workspaceKey(), cmd.projectKey(), actor.memberId());

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());

        Set<Long> targetMemberIds = cmd.extractMemberIds();
        Map<Long, ProjectRole> roleMap = cmd.extractRoleMap();

        List<WorkspaceMember> workspaceMembers = workspaceMemberFinder.getAllBy(targetMemberIds, cmd.workspaceKey());

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
                pm, workspace, project, JoinMethod.BY_ADMIN, actor.memberId(), actor.displayName()));

        return ProjectMembersCommandResult.of(project, newMembers);
    }

    @Override
    public ProjectMemberCommandResult joinViaDirect(DirectJoinProjectCommand cmd) {
        WorkspaceMemberInfo actor = cmd.actor();
        projectAuthService.requireDirectJoinPermission(cmd.workspaceKey(), cmd.projectKey(), actor.memberId());

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        WorkspaceMember workspaceMember = workspaceMemberFinder.getBy(actor.memberId(), cmd.workspaceKey());

        projectValidator.ensureNotAlreadyJoined(project, actor.memberId());

        ProjectMember projectMember = ProjectMember.create(project, workspaceMember, project.getDefaultJoinRole());
        projectMemberRepository.save(projectMember);

        eventPublisher.publishMemberJoinedProject(
                projectMember, workspace, project, JoinMethod.DIRECT, actor.memberId(), actor.displayName());

        return ProjectMemberCommandResult.of(projectMember);
    }

    @Override
    public ProjectMemberCommandResult leave(String workspaceKey, String projectKey, Long actorMemberId) {
        projectAuthService.requireProjectViewer(workspaceKey, projectKey, actorMemberId);

        Project project = projectFinder.getModifiableBy(projectKey, workspaceKey);
        ProjectMember actor = projectMemberFinder.getBy(project, actorMemberId);

        actor.remove();

        // TODO: ProjectMemberLeftEvent

        return ProjectMemberCommandResult.of(actor);
    }

    @Override
    public ProjectMemberCommandResult kickMember(KickProjectMemberCommand cmd) {
        WorkspaceMemberInfo actor = cmd.actor();
        projectAuthService.requireProjectAdmin(cmd.workspaceKey(), cmd.projectKey(), actor.memberId());

        if (actor.memberId().equals(cmd.targetMemberId())) {
            throw new BadRequestException(ProjectErrorCode.SELF_KICK_NOT_ALLOWED);
        }

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        ProjectMember target = projectMemberFinder.getBy(project, cmd.targetMemberId());

        target.remove();

        // TODO: ProjectMemberKickedEvent

        return ProjectMemberCommandResult.of(target);
    }

    @Override
    public ProjectMemberCommandResult changeProjectRole(ChangeProjectRoleCommand cmd) {
        WorkspaceMemberInfo actor = cmd.actor();
        projectAuthService.requireRoleGrantPermission(
                cmd.workspaceKey(), cmd.projectKey(), cmd.newRole(), actor.memberId());

        if (actor.memberId().equals(cmd.targetMemberId())) {
            throw new BadRequestException(ProjectErrorCode.SELF_ROLE_MODIFICATION_NOT_ALLOWED);
        }

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        ProjectMember target = projectMemberFinder.getBy(project, cmd.targetMemberId());

        ProjectRole oldRole = target.getRole();
        target.changeRole(cmd.newRole());

        eventPublisher.publishProjectRoleChanged(target, oldRole, cmd.newRole(), actor.memberId(), actor.displayName());

        return ProjectMemberCommandResult.of(target);
    }

    // TODO: add javadoc about the next information
    //  - is not a UseCase
    //  - is called from another service(internal usage)
    public void join(Project project, WorkspaceMember workspaceMember, ProjectRole role, JoinMethod joinMethod) {
        if (projectMemberFinder.existsBy(project, workspaceMember.getMemberId())) {
            return;
        }

        ProjectMember projectMember = ProjectMember.create(project, workspaceMember, role);
        projectMemberRepository.save(projectMember);

        eventPublisher.publishMemberJoinedProject(
                projectMember,
                project.getWorkspace(),
                project,
                joinMethod,
                workspaceMember.getMemberId(),
                workspaceMember.getDisplayName());
    }
}
