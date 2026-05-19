package com.tissue.feature.workspace.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.service.ProjectJoinService;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.workspace.application.dto.response.query.InvitationDetail;
import com.tissue.feature.workspace.application.port.repository.InvitationCommandRepository;
import com.tissue.feature.workspace.application.port.repository.InvitationQueryRepository;
import com.tissue.feature.workspace.application.port.usecase.InvitationUseCase;
import com.tissue.feature.workspace.application.service.finder.InvitationFinder;
import com.tissue.feature.workspace.domain.Invitation;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class InvitationService implements InvitationUseCase {

    private final InvitationFinder invitationFinder;
    private final MemberFinder memberFinder;
    private final ProjectFinder projectFinder;
    private final WorkspaceJoinService workspaceJoinService;
    private final ProjectJoinService projectJoinService;
    private final InvitationQueryRepository invitationQueryRepository;
    private final InvitationCommandRepository invitationCommandRepository;

    @Override
    public void accept(Long memberId, Long invitationId) {
        Member member = memberFinder.getActiveById(memberId);
        Invitation invitation = invitationFinder.getBy(invitationId, member);

        invitation.ensureEditable();

        WorkspaceMember joinedWorkspaceMember =
                workspaceJoinService.join(invitation.getWorkspace(), member, invitation.getWorkspaceRole());

        if (invitation.projectKeysNotEmpty()) {
            joinProjects(invitation, joinedWorkspaceMember);
        }

        invitationCommandRepository.delete(invitation);

        log.info(
                "Member(id={}) joined Workspace(key={}) via Invitation(id={})",
                memberId,
                invitation.getWorkspaceKey(),
                invitationId);
    }

    @Override
    public void reject(Long memberId, Long invitationId) {
        Member member = memberFinder.getActiveById(memberId);
        Invitation invitation = invitationFinder.getBy(invitationId, member);

        invitationCommandRepository.delete(invitation);

        log.info(
                "Member(id={}) rejected Invitation(id={}) to Workspace(key={})",
                memberId,
                invitationId,
                invitation.getWorkspaceKey());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationDetail> getMyInvitations(Long memberId) {
        // TODO: N+1, consider optimization
        return invitationQueryRepository.findAllByMemberId(memberId).stream()
                .map(invitation -> {
                    Member inviter = memberFinder
                            .getOptionalActiveById(invitation.getCreatedBy())
                            .orElse(null);
                    return InvitationDetail.from(invitation, inviter);
                })
                .toList();
    }

    private void joinProjects(Invitation invitation, WorkspaceMember workspaceMember) {
        List<String> projectKeys = invitation.getProjectKeys();
        for (var projectKey : projectKeys) {
            projectFinder
                    .getOptionalBy(invitation.getWorkspaceKey(), projectKey)
                    .ifPresent(project -> {
                        projectJoinService.join(project, workspaceMember);
                    });
        }
    }
}
