package com.tissue.api.workspacemember.application.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.position.domain.Position;
import com.tissue.api.position.application.service.command.PositionReader;
import com.tissue.api.team.domain.Team;
import com.tissue.api.team.application.service.command.TeamReader;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.enums.WorkspaceRole;
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

	private final WorkspaceMemberReader workspaceMemberReader;
	private final PositionReader positionReader;
	private final TeamReader teamReader;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceMemberPermissionValidator workspaceMemberPermissionValidator;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public WorkspaceMemberResponse updateDisplayName(
		String workspaceCode,
		Long memberId,
		UpdateDisplayNameRequest request
	) {
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

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
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterMemberId, workspaceCode);
		WorkspaceMember target = workspaceMemberReader.findWorkspaceMember(targetMemberId, workspaceCode);

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
		Position position = positionReader.findPosition(positionId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(targetMemberId, workspaceCode);

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
		Position position = positionReader.findPosition(positionId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(targetMemberId, workspaceCode);

		workspaceMember.removePosition(position);
	}

	@Transactional
	public WorkspaceMemberResponse setTeam(
		String workspaceCode,
		Long teamId,
		Long targetMemberId,
		Long loginMemberId
	) {
		Team team = teamReader.findTeam(teamId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(targetMemberId, workspaceCode);

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
		Team team = teamReader.findTeam(teamId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(targetMemberId, workspaceCode);

		workspaceMember.removeTeam(team);
	}

	@Transactional
	public TransferOwnershipResponse transferWorkspaceOwnership(
		String workspaceCode,
		Long targetMemberId,
		Long requesterMemberId
	) {
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterMemberId, workspaceCode);
		WorkspaceMember target = workspaceMemberReader.findWorkspaceMember(targetMemberId, workspaceCode);

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
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterMemberId, workspaceCode);
		WorkspaceMember target = workspaceMemberReader.findWorkspaceMember(targetMemberId, workspaceCode);

		workspaceMemberPermissionValidator.validateCanRemoveWorkspaceMember(requester, target);

		target.remove();
	}
}
