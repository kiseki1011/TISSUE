package com.tissue.workspace.application.service;

import com.tissue.member.application.service.finder.MemberFinder;
import com.tissue.project.application.service.ProjectMemberCommandService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
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
import com.tissue.workspace.application.service.command.WorkspaceParticipationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.ProjectJoinConfig;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceInviteLink;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.WorkspaceExceptions;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceInviteLinkService implements WorkspaceInviteLinkUseCase {

    private final WorkspaceFinder workspaceFinder;
    private final ProjectFinder projectFinder;
    private final MemberFinder memberFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WorkspaceLinkCommandRepository linkRepository;
    private final WorkspaceLinkQueryRepository linkQueryRepository;
    private final WorkspaceParticipationService workspaceParticipationService;
    private final ProjectMemberCommandService projectMemberCommandService;

    @Override
    @Transactional
    public String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd) {
        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());

        return saveLink(workspace, cmd.workspaceRole(), cmd.targetProjects(), cmd.expiredAt());
    }

    @Override
    @Transactional
    public String createProjectLink(CreateProjectInviteLinkCommand cmd) {
        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());

        var projectJoinConfig = new ProjectJoinConfigDto(cmd.projectKey(), cmd.role());
        List<ProjectJoinConfigDto> singleProjectConfig = List.of(projectJoinConfig);

        return saveLink(workspace, WorkspaceRole.MEMBER, singleProjectConfig, cmd.expiredAt());
    }

    @Override
    @Transactional
    public void expireLink(ExpireLinkCommand cmd) {
        WorkspaceInviteLink link =
                linkQueryRepository
                        .findByToken(cmd.token())
                        .orElseThrow(
                                () ->
                                        WorkspaceExceptions.linkNotFound(
                                                cmd.workspaceKey(), cmd.token()));

        link.expire();
    }

    @Override
    @Transactional
    public WorkspaceMemberResponse joinViaLink(JoinViaLinkCommand cmd) {
        WorkspaceInviteLink link =
                linkQueryRepository
                        .findByToken(cmd.token())
                        .orElseThrow(
                                () ->
                                        WorkspaceExceptions.linkNotFound(
                                                cmd.workspaceKey(), cmd.token()));

        if (!link.isValid()) {
            throw WorkspaceExceptions.invalidLink(link);
        }

        WorkspaceMember workspaceMember =
                workspaceParticipationService.join(
                        link.getWorkspace(),
                        memberFinder.getActiveBy(cmd.memberId()),
                        link.getWorkspaceRole());

        List<ProjectJoinConfig> projectConfigs = link.getProjectConfigs();

        if (link.projectConfigsNotEmpty()) {
            joinProjects(projectConfigs, workspaceMember);
        }

        return WorkspaceMemberResponse.from(workspaceMember);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceInviteLinkDetail getLinkInfo(String workspaceKey, String token) {
        WorkspaceInviteLink link =
                linkQueryRepository
                        .findByToken(token)
                        .orElseThrow(() -> WorkspaceExceptions.linkNotFound(workspaceKey, token));

        if (!link.isValid()) {
            throw WorkspaceExceptions.invalidLink(link);
        }

        WorkspaceMember linkCreator =
                workspaceMemberFinder.findBy(link.getCreatedBy(), workspaceKey);

        return WorkspaceInviteLinkDetail.of(link, linkCreator);
    }

    private String saveLink(
            Workspace workspace,
            WorkspaceRole roleToGrant,
            List<ProjectJoinConfigDto> targetProjects,
            Instant expiredAt) {
        String token = UUID.randomUUID().toString();

        WorkspaceInviteLink link =
                WorkspaceInviteLink.create(workspace, token, roleToGrant, expiredAt);

        addProjectsToLink(workspace.getKey(), targetProjects, link);

        linkRepository.save(link);
        return token;
    }

    private void addProjectsToLink(
            String workspaceKey,
            List<ProjectJoinConfigDto> targetProjects,
            WorkspaceInviteLink link) {
        if (targetProjects != null) {
            for (var dto : targetProjects) {
                Project project = projectFinder.getModifiableBy(dto.projectKey(), workspaceKey);
                link.addProjectConfig(project, dto.role());
            }
        }
    }

    private void joinProjects(List<ProjectJoinConfig> configs, WorkspaceMember workspaceMember) {
        for (ProjectJoinConfig config : configs) {
            projectFinder
                    .findOptionalBy(config.projectId())
                    .ifPresent(
                            project -> {
                                projectMemberCommandService.addMember(
                                        project, workspaceMember.getMemberId(), config.role());
                            });
        }
    }
}
