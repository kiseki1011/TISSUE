package com.tissue.workspace.application.service;

import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.exception.InsufficientProjectRoleException;
import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.CreateProjectInviteLinkCommand;
import com.tissue.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.application.dto.request.ExpireLinkCommand;
import com.tissue.workspace.application.port.in.WorkspaceLinkUseCase;
import com.tissue.workspace.application.port.out.WorkspaceLinkCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceLinkQueryRepository;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceInviteLink;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.WorkspaceInviteLinkNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkspaceLinkService implements WorkspaceLinkUseCase {

    private final WorkspaceFinder workspaceFinder;
    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final WorkspaceLinkCommandRepository linkRepository;
    private final WorkspaceLinkQueryRepository linkQueryRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    @Override
    public String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthorizationService.requireWorkspaceAdmin(actorContext);

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());

        return saveLink(workspace, cmd.workspaceRole(), cmd.targetProjects(), cmd.expiredAt());
    }

    /**
     * Creates a project invite link.
     *
     * <p>Authorization rules:
     * <ul>
     *   <li>{@link WorkspaceRole#ADMIN} is always allowed, even if not a member of the project</li>
     *   <li>{@link ProjectRole#ADMIN} is allowed</li>
     *   <li>All other roles are denied</li>
     * </ul>
     *
     * <p>This logic is intentionally implemented locally instead of using
     * {@link ProjectAuthorizationService} because it allows Workspace ADMINs
     * who are not project members.
     */
    @Override
    public String createProjectLink(CreateProjectInviteLinkCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();

        if (actorContext.isWorkspaceAdmin() || isProjectAdmin(cmd, actorContext)) {
            Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());

            var projectJoinConfig = new ProjectJoinConfigDto(cmd.projectKey(), cmd.role());
            List<ProjectJoinConfigDto> singleProjectConfig = List.of(projectJoinConfig);

            return saveLink(workspace, WorkspaceRole.MEMBER, singleProjectConfig, cmd.expiredAt());
        }

        throw new InsufficientProjectRoleException(actorContext.workspaceKey(), cmd.projectKey(), ProjectRole.ADMIN);
    }

    @Override
    public void expireLink(ExpireLinkCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();

        WorkspaceInviteLink link = linkQueryRepository
                .findByToken(cmd.token())
                .orElseThrow(() -> new WorkspaceInviteLinkNotFoundException(actorContext.workspaceKey(), cmd.token()));

        workspaceAuthorizationService.requireInviteLinkEditPermission(link, actorContext);

        link.expire();
    }

    private boolean isProjectAdmin(CreateProjectInviteLinkCommand cmd, WorkspaceMemberContext actorContext) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), actorContext.workspaceKey());
        ProjectMember actor = projectMemberFinder.getActive(project, actorContext.memberId());
        return actor.getRole().isAdmin();
    }

    private String saveLink(
            Workspace workspace,
            WorkspaceRole roleToGrant,
            @Nullable List<ProjectJoinConfigDto> projectJoinConfigs,
            @Nullable Instant expiredAt) {

        String token = UUID.randomUUID().toString();
        WorkspaceInviteLink link = WorkspaceInviteLink.create(workspace, token, roleToGrant, expiredAt);

        addProjectsToLink(workspace.getKey(), projectJoinConfigs, link);

        linkRepository.save(link);
        return token;
    }

    private void addProjectsToLink(
            String workspaceKey, @Nullable List<ProjectJoinConfigDto> projectJoinConfigs, WorkspaceInviteLink link) {

        if (projectJoinConfigs != null) {
            for (var joinConfig : projectJoinConfigs) {
                Project project = projectFinder.getModifiableBy(joinConfig.projectKey(), workspaceKey);
                link.addProjectConfig(project, joinConfig.role());
            }
        }
    }
}
