package com.tissue.api.workspacemember.application.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.position.domain.model.Position;
import com.tissue.api.position.application.service.command.PositionFinder;
import com.tissue.api.team.domain.model.Team;
import com.tissue.api.team.application.service.command.TeamFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
import com.tissue.api.workspacemember.domain.event.WorkspaceMemberRoleChangedEvent;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.domain.service.WorkspaceMemberPermissionValidator;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateDisplayNameRequest;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateRoleRequest;
import com.tissue.api.workspacemember.presentation.dto.response.TransferOwnershipResponse;
import com.tissue.api.workspacemember.presentation.dto.response.WorkspaceMemberResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceMemberCommandService {

	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final PositionFinder positionFinder;
	private final TeamFinder teamFinder;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceMemberPermissionValidator workspaceMemberPermissionValidator;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public WorkspaceMemberResponse updateDisplayName(
		String workspaceCode,
		Long memberId,
		UpdateDisplayNameRequest request
	) {
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceCode);

		workspaceMember.updateDisplayName(request.displayName());
		workspaceMemberRepository.saveAndFlush(workspaceMember);

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public WorkspaceMemberResponse updateRole(
		String workspaceCode,
		Long targetMemberId,
		Long requesterMemberId,
		UpdateRoleRequest request
	) {
		WorkspaceMember requester = workspaceMemberFinder.findWorkspaceMember(requesterMemberId, workspaceCode);
		WorkspaceMember target = workspaceMemberFinder.findWorkspaceMember(targetMemberId, workspaceCode);

		workspaceMemberPermissionValidator.validateCanUpdateRole(requester, target);

		WorkspaceRole oldRole = target.getRole();
		target.updateRole(request.updateWorkspaceRole());

		eventPublisher.publishEvent(
			WorkspaceMemberRoleChangedEvent.createEvent(target, oldRole, requesterMemberId)
		);

		return WorkspaceMemberResponse.from(target);
	}

	@Transactional
	public WorkspaceMemberResponse setPosition(
		String workspaceCode,
		Long positionId,
		Long targetMemberId,
		Long loginMemberId
	) {
		Position position = positionFinder.findPosition(positionId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(targetMemberId, workspaceCode);

		workspaceMember.addPosition(position);

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public void removePosition(
		String workspaceCode,
		Long positionId,
		Long targetMemberId,
		Long loginMemberId
	) {
		Position position = positionFinder.findPosition(positionId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(targetMemberId, workspaceCode);

		workspaceMember.removePosition(position);
	}

	@Transactional
	public WorkspaceMemberResponse setTeam(
		String workspaceCode,
		Long teamId,
		Long targetMemberId,
		Long loginMemberId
	) {
		Team team = teamFinder.findTeam(teamId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(targetMemberId, workspaceCode);

		workspaceMember.addTeam(team);

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public void removeTeam(
		String workspaceCode,
		Long teamId,
		Long targetMemberId,
		Long loginMemberId
	) {
		Team team = teamFinder.findTeam(teamId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(targetMemberId, workspaceCode);

		workspaceMember.removeTeam(team);
	}

	@Transactional
	public TransferOwnershipResponse transferWorkspaceOwnership(
		String workspaceCode,
		Long targetMemberId,
		Long requesterMemberId
	) {
		WorkspaceMember requester = workspaceMemberFinder.findWorkspaceMember(requesterMemberId, workspaceCode);
		WorkspaceMember target = workspaceMemberFinder.findWorkspaceMember(targetMemberId, workspaceCode);

		requester.updateRoleToAdmin();
		target.updateRoleToOwner();

		return TransferOwnershipResponse.from(requester, target);
	}

	@Transactional
	public void removeWorkspaceMember(
		String workspaceCode,
		Long targetMemberId,
		Long requesterMemberId
	) {
		WorkspaceMember requester = workspaceMemberFinder.findWorkspaceMember(requesterMemberId, workspaceCode);
		WorkspaceMember target = workspaceMemberFinder.findWorkspaceMember(targetMemberId, workspaceCode);

		workspaceMemberPermissionValidator.validateCanRemoveWorkspaceMember(requester, target);

		target.remove();
	}
}
