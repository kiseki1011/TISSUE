package com.tissue.workspace.application.service.command;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tissue.member.application.service.finder.MemberFinder;
import com.tissue.member.domain.Member;
import com.tissue.project.application.service.ProjectMemberCommandService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.workspace.application.dto.response.query.InvitationDetail;
import com.tissue.workspace.application.port.in.InvitationUseCase;
import com.tissue.workspace.application.port.out.InvitationQueryRepository;
import com.tissue.workspace.application.service.finder.InvitationFinder;
import com.tissue.workspace.domain.Invitation;
import com.tissue.workspace.domain.ProjectJoinConfig;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.InvitationStatus;
import com.tissue.workspace.domain.exception.InvitationAlreadyProcessedException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationService implements InvitationUseCase {

	private final InvitationFinder invitationFinder;
	private final MemberFinder memberFinder;
	private final ProjectFinder projectFinder;
	private final WorkspaceParticipationService workspaceParticipationService;
	private final ProjectMemberCommandService projectMemberCommandService;
	private final InvitationQueryRepository invitationQueryRepository;

	@Override
	public void accept(Long memberId, Long invitationId) {
		Invitation invitation = invitationFinder.findBy(invitationId);

		if (invitation.isProcessed()) {
			throw new InvitationAlreadyProcessedException(invitationId, invitation.getStatus());
		}

		invitation.accept();

		WorkspaceMember workspaceMember = workspaceParticipationService.join(
			invitation.getWorkspace(),
			memberFinder.findMemberById(memberId),
			invitation.getWorkspaceRole()
		);

		List<ProjectJoinConfig> projectConfigs = invitation.getProjectConfigs();

		if (invitation.projectConfigsNotEmpty()) {
			joinProjects(projectConfigs, workspaceMember);
		}

		// TODO: InvitationAcceptedEvent
	}

	@Override
	public void reject(Long memberId, Long invitationId) {
		Invitation invitation = invitationFinder.findBy(invitationId);

		if (invitation.isProcessed()) {
			throw new InvitationAlreadyProcessedException(invitationId, invitation.getStatus());
		}

		invitation.reject();

		// TODO: InvitationRejectedEvent
	}

	@Override
	public List<InvitationDetail> getMyInvitations(Long memberId) {
		// TODO: N+1 발생, 최적화 고려
		return invitationQueryRepository.findAllByMemberIdAndStatus(memberId, InvitationStatus.PENDING)
			.stream()
			.map(invitation -> {
				Member inviter = memberFinder.findOptionalBy(invitation.getCreatedBy())
					.orElse(null);
				return InvitationDetail.from(invitation, inviter);
			})
			.toList();
	}

	private void joinProjects(List<ProjectJoinConfig> configs, WorkspaceMember workspaceMember) {
		for (ProjectJoinConfig config : configs) {
			projectFinder.findOptionalBy(config.projectId())
				.ifPresent(project -> {
					projectMemberCommandService.addMember(project, workspaceMember.getMemberId(), config.role());
				});
		}
	}
}
