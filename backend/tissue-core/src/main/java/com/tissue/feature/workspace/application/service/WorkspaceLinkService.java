package com.tissue.feature.workspace.application.service;

import static com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode.INVALID_INVITE_LINK;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.project.application.service.ProjectJoinService;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceMemberResponse;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;
import com.tissue.feature.workspace.application.port.repository.WorkspaceLinkCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceLinkQueryRepository;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceLinkUseCase;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceInviteLink;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.WorkspaceInviteLinkNotFoundException;
import com.tissue.shared.exception.base.BadRequestException;
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

    private final MemberFinder memberFinder;
    private final ProjectFinder projectFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WorkspaceLinkCommandRepository linkRepository;
    private final WorkspaceLinkQueryRepository linkQueryRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WorkspaceJoinProcessor workspaceJoinProcessor;
    private final ProjectJoinService projectJoinService;

    @Override
    public String createWorkspaceLink(String workspaceKey, CreateWorkspaceInviteLinkCommand cmd, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        return saveLink(actor.getWorkspace(), cmd.workspaceRole(), cmd.targetProjectKeys(), cmd.expiredAt());
    }

    @Override
    public void deleteLink(String workspaceKey, String token, Long actorMemberId) {
        WorkspaceInviteLink link = linkQueryRepository
                .findByToken(token)
                .orElseThrow(() -> new WorkspaceInviteLinkNotFoundException(workspaceKey, token));

        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireInviteLinkEditPermission(link, actor);

        linkRepository.delete(link);
    }

    private String saveLink(
            Workspace workspace,
            WorkspaceRole roleToGrant,
            @Nullable List<String> projectKeys,
            @Nullable Instant expiredAt) {

        String token = UUID.randomUUID().toString();
        WorkspaceInviteLink link = WorkspaceInviteLink.create(workspace, token, roleToGrant, expiredAt);

        addProjectsToLink(workspace.getKey(), projectKeys, link);

        linkRepository.save(link);
        return token;
    }

    @Override
    public WorkspaceMemberResponse joinViaLink(String workspaceKey, String token, Long actorMemberId) {
        WorkspaceInviteLink link = linkQueryRepository
                .findByToken(token)
                .orElseThrow(() -> new WorkspaceInviteLinkNotFoundException(workspaceKey, token));

        if (linkIsInvalid(link)) {
            throw new BadRequestException(INVALID_INVITE_LINK);
        }

        WorkspaceMember workspaceMember = workspaceJoinProcessor.processJoin(
                link.getWorkspace(), memberFinder.getActiveBy(actorMemberId), link.getWorkspaceRole());

        List<String> projectKeys = link.getProjectKeys();

        if (link.projectKeysNotEmpty()) {
            joinProjects(projectKeys, workspaceMember);
        }

        return WorkspaceMemberResponse.from(workspaceMember);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceInviteLinkDetail getLinkDetail(String workspaceKey, String token, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        WorkspaceInviteLink link = linkQueryRepository
                .findByToken(token)
                .orElseThrow(() -> new WorkspaceInviteLinkNotFoundException(workspaceKey, token));

        WorkspaceMember linkCreator = workspaceMemberFinder.getWithWorkspace(workspaceKey, link.getCreatedBy());

        return WorkspaceInviteLinkDetail.of(link, linkCreator);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceInviteLinkDetail> getWorkspaceLinks(String workspaceKey, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        List<WorkspaceInviteLink> links = linkQueryRepository.findAllByWorkspaceKey(workspaceKey);

        return links.stream()
                .map(link -> {
                    WorkspaceMember linkCreator =
                            workspaceMemberFinder.getWithWorkspace(workspaceKey, link.getCreatedBy());
                    return WorkspaceInviteLinkDetail.of(link, linkCreator);
                })
                .toList();
    }

    private void joinProjects(List<String> projectKeys, WorkspaceMember workspaceMember) {
        for (var projectKey : projectKeys) {
            projectFinder
                    .getOptionalBy(workspaceMember.getWorkspaceKey(), projectKey)
                    .ifPresent(project -> {
                        projectJoinService.join(project, workspaceMember);
                    });
        }
    }

    private void addProjectsToLink(String workspaceKey, @Nullable List<String> projectKeys, WorkspaceInviteLink link) {
        if (projectKeys == null) {
            return;
        }
        for (var projectKey : projectKeys) {
            projectFinder.getWithWorkspaceBy(workspaceKey, projectKey);
            link.addProjectKey(projectKey);
        }
    }

    private boolean linkIsInvalid(WorkspaceInviteLink link) {
        return !link.isValid();
    }
}
