package com.tissue.workspace.application.service;

import com.tissue.member.application.service.MemberFinder;
import com.tissue.project.application.service.ProjectJoinService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.workspace.application.dto.request.JoinViaLinkCommand;
import com.tissue.workspace.application.dto.response.command.WorkspaceMemberResponse;
import com.tissue.workspace.application.port.in.WorkspaceLinkJoinUseCase;
import com.tissue.workspace.application.port.out.WorkspaceLinkQueryRepository;
import com.tissue.workspace.domain.WorkspaceInviteLink;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.exception.InvalidWorkspaceInviteLinkException;
import com.tissue.workspace.domain.exception.WorkspaceInviteLinkNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
                link.getWorkspace(), memberFinder.getActiveBy(cmd.actorMemberId()), link.getWorkspaceRole());

        List<String> projectKeys = link.getProjectKeys();

        if (link.projectKeysNotEmpty()) {
            joinProjects(projectKeys, workspaceMember);
        }

        // TODO: eventPublisher.publishJoinedViaLink

        return WorkspaceMemberResponse.from(workspaceMember);
    }

    private void joinProjects(List<String> projectKeys, WorkspaceMember workspaceMember) {
        for (var projectKey : projectKeys) {
            projectFinder
                    .getOptionalBy(projectKey, workspaceMember.getWorkspaceKey())
                    .ifPresent(project -> {
                        projectJoinService.join(project, workspaceMember);
                    });
        }
    }
}
