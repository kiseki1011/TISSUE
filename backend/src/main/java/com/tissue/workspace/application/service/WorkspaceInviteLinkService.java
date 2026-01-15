package com.tissue.workspace.application.service;

import com.tissue.common.enums.JoinMethod;
import com.tissue.member.application.service.finder.MemberFinder;
import com.tissue.project.application.service.ProjectParticipationService;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import com.tissue.security.authentication.application.port.out.CurrentMemberProvider;
import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.application.dto.in.CreateProjectInviteLinkCommand;
import com.tissue.workspace.application.dto.in.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.application.dto.in.ExpireLinkCommand;
import com.tissue.workspace.application.dto.in.JoinViaLinkCommand;
import com.tissue.workspace.application.dto.out.command.WorkspaceMemberResponse;
import com.tissue.workspace.application.dto.out.query.WorkspaceInviteLinkDetail;
import com.tissue.workspace.application.port.in.WorkspaceInviteLinkUseCase;
import com.tissue.workspace.application.port.out.WorkspaceLinkCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceLinkQueryRepository;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.ProjectJoinConfig;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceInviteLink;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.InvalidWorkspaceInviteLinkException;
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
public class WorkspaceInviteLinkService implements WorkspaceInviteLinkUseCase {

    private final WorkspaceFinder workspaceFinder;
    private final ProjectFinder projectFinder;
    private final MemberFinder memberFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WorkspaceLinkCommandRepository linkRepository;
    private final WorkspaceLinkQueryRepository linkQueryRepository;
    private final WorkspaceParticipationService workspaceParticipationService;
    private final ProjectParticipationService projectMemberCommandService;
    private final ProjectAuthorizationService projectAuthService;
    private final WorkspaceAuthorizationService workspaceAuthService;
    private final CurrentMemberProvider currentMemberProvider;

    @Override
    public String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceAdmin(cmd.workspaceKey(), actorMemberId);

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());

        return saveLink(workspace, cmd.workspaceRole(), cmd.targetProjects(), cmd.expiredAt());
    }

    @Override
    public String createProjectLink(CreateProjectInviteLinkCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.requireProjectAdmin(cmd.workspaceKey(), cmd.projectKey(), actorMemberId);

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());

        var projectJoinConfig = new ProjectJoinConfigDto(cmd.projectKey(), cmd.role());
        List<ProjectJoinConfigDto> singleProjectConfig = List.of(projectJoinConfig);

        return saveLink(workspace, WorkspaceRole.MEMBER, singleProjectConfig, cmd.expiredAt());
    }

    @Override
    public void expireLink(ExpireLinkCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireInviteLinkEditPermission(cmd.workspaceKey(), cmd.token(), actorMemberId);

        WorkspaceInviteLink link = linkQueryRepository
                .findByToken(cmd.token())
                .orElseThrow(() -> new WorkspaceInviteLinkNotFoundException(cmd.workspaceKey(), cmd.token()));

        link.expire();
    }

    @Override
    public WorkspaceMemberResponse joinViaLink(JoinViaLinkCommand cmd) {
        WorkspaceInviteLink link = linkQueryRepository
                .findByToken(cmd.token())
                .orElseThrow(() -> new WorkspaceInviteLinkNotFoundException(cmd.workspaceKey(), cmd.token()));

        if (!link.isValid()) {
            throw new InvalidWorkspaceInviteLinkException(link);
        }

        WorkspaceMember workspaceMember = workspaceParticipationService.join(
                link.getWorkspace(),
                memberFinder.getActiveBy(cmd.memberId()),
                link.getWorkspaceRole(),
                cmd.memberId(),
                JoinMethod.LINK);

        List<ProjectJoinConfig> projectConfigs = link.getProjectConfigs();

        if (link.projectConfigsNotEmpty()) {
            joinProjects(projectConfigs, workspaceMember);
        }

        return WorkspaceMemberResponse.from(workspaceMember);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceInviteLinkDetail getLinkInfo(String workspaceKey, String token) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceMember(workspaceKey, actorMemberId);

        WorkspaceInviteLink link = linkQueryRepository
                .findByToken(token)
                .orElseThrow(() -> new WorkspaceInviteLinkNotFoundException(workspaceKey, token));

        if (!link.isValid()) {
            throw new InvalidWorkspaceInviteLinkException(link);
        }

        WorkspaceMember linkCreator = workspaceMemberFinder.getBy(link.getCreatedBy(), workspaceKey);

        return WorkspaceInviteLinkDetail.of(link, linkCreator);
    }

    private String saveLink(
            Workspace workspace,
            WorkspaceRole roleToGrant,
            @Nullable List<ProjectJoinConfigDto> targetProjects,
            @Nullable Instant expiredAt) {

        String token = UUID.randomUUID().toString();
        WorkspaceInviteLink link = WorkspaceInviteLink.create(workspace, token, roleToGrant, expiredAt);

        addProjectsToLink(workspace.getKey(), targetProjects, link);

        linkRepository.save(link);
        return token;
    }

    private void addProjectsToLink(
            String workspaceKey, @Nullable List<ProjectJoinConfigDto> targetProjects, WorkspaceInviteLink link) {

        if (targetProjects != null) {
            for (var dto : targetProjects) {
                Project project = projectFinder.getModifiableBy(dto.projectKey(), workspaceKey);
                link.addProjectConfig(project, dto.role());
            }
        }
    }

    private void joinProjects(List<ProjectJoinConfig> configs, WorkspaceMember workspaceMember) {
        for (ProjectJoinConfig config : configs) {
            projectFinder.getOptionalBy(config.projectId()).ifPresent(project -> {
                projectMemberCommandService.join(
                        project, workspaceMember.getMemberId(), config.role(), JoinMethod.LINK);
            });
        }
    }
}
