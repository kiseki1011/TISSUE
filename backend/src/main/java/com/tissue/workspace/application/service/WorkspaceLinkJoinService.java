package com.tissue.workspace.application.service;

import com.tissue.common.enums.JoinMethod;
import com.tissue.member.application.service.finder.MemberFinder;
import com.tissue.project.application.service.ProjectJoinService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.application.dto.in.JoinViaLinkCommand;
import com.tissue.workspace.application.dto.out.command.WorkspaceMemberResponse;
import com.tissue.workspace.application.port.in.WorkspaceLinkJoinUseCase;
import com.tissue.workspace.application.port.out.WorkspaceLinkQueryRepository;
import com.tissue.workspace.domain.ProjectJoinConfig;
import com.tissue.workspace.domain.WorkspaceInviteLink;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.exception.InvalidWorkspaceInviteLinkException;
import com.tissue.workspace.domain.exception.WorkspaceInviteLinkNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkspaceLinkJoinService implements WorkspaceLinkJoinUseCase {

    private final MemberFinder memberFinder;
    private final ProjectFinder projectFinder;
    private final WorkspaceParticipationService workspaceParticipationService;
    private final ProjectJoinService projectJoinService;
    private final WorkspaceLinkQueryRepository linkQueryRepository;

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
                memberFinder.getActiveBy(cmd.actorMemberId()),
                link.getWorkspaceRole(),
                cmd.actorMemberId(),
                null,
                JoinMethod.LINK);

        List<ProjectJoinConfig> projectConfigs = link.getProjectConfigs();

        if (link.projectConfigsNotEmpty()) {
            joinProjects(projectConfigs, workspaceMember);
        }

        return WorkspaceMemberResponse.from(workspaceMember);
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
                projectJoinService.join(project, workspaceMember, config.role(), JoinMethod.LINK);
            });
        }
    }
}
