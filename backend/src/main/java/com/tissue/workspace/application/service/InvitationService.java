package com.tissue.workspace.application.service;

import com.tissue.member.application.service.MemberFinder;
import com.tissue.member.domain.Member;
import com.tissue.project.application.service.ProjectJoinService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.workspace.application.dto.response.query.InvitationDetail;
import com.tissue.workspace.application.port.in.InvitationUseCase;
import com.tissue.workspace.application.port.out.InvitationQueryRepository;
import com.tissue.workspace.application.service.finder.InvitationFinder;
import com.tissue.workspace.domain.Invitation;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.InvitationStatus;
import com.tissue.workspace.domain.exception.InvitationAlreadyProcessedException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class InvitationService implements InvitationUseCase {

    private final InvitationFinder invitationFinder;
    private final MemberFinder memberFinder;
    private final ProjectFinder projectFinder;
    private final WorkspaceParticipationService workspaceParticipationService;
    private final ProjectJoinService projectJoinService;
    private final InvitationQueryRepository invitationQueryRepository;

    @Override
    public void accept(Long memberId, Long invitationId) {
        Member member = memberFinder.getActiveBy(memberId);
        Invitation invitation = invitationFinder.getBy(invitationId, member);

        if (invitation.isProcessed()) {
            throw new InvitationAlreadyProcessedException(invitation);
        }

        invitation.accept();

        WorkspaceMember joinedWorkspaceMember = workspaceParticipationService.join(
                invitation.getWorkspace(), memberFinder.getActiveBy(memberId), invitation.getWorkspaceRole());

        if (invitation.projectKeysNotEmpty()) {
            joinProjects(invitation, joinedWorkspaceMember);
        }

        // TODO: eventPublisher.publishJoinedViaInvitation
    }

    @Override
    public void reject(Long memberId, Long invitationId) {
        Member member = memberFinder.getActiveBy(memberId);
        Invitation invitation = invitationFinder.getBy(invitationId, member);

        if (invitation.isProcessed()) {
            throw new InvitationAlreadyProcessedException(invitation);
        }

        invitation.reject();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationDetail> getMyInvitations(Long memberId) {
        // TODO: N+1, consider optimization
        return invitationQueryRepository.findAllByMemberIdAndStatus(memberId, InvitationStatus.PENDING).stream()
                .map(invitation -> {
                    Member inviter = memberFinder
                            .getOptActiveBy(invitation.getCreatedBy())
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
