package com.tissue.api.workspacemember.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.position.domain.Position;
import com.tissue.api.position.service.command.PositionReader;
import com.tissue.api.team.domain.Team;
import com.tissue.api.team.service.command.TeamReader;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.domain.event.WorkspaceMemberRoleChangedEvent;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateDisplayNameRequest;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateRoleRequest;
import com.tissue.api.workspacemember.presentation.dto.response.AssignPositionResponse;
import com.tissue.api.workspacemember.presentation.dto.response.AssignTeamResponse;
import com.tissue.api.workspacemember.presentation.dto.response.RemoveWorkspaceMemberResponse;
import com.tissue.api.workspacemember.presentation.dto.response.TransferOwnershipResponse;
import com.tissue.api.workspacemember.presentation.dto.response.UpdateNicknameResponse;
import com.tissue.api.workspacemember.presentation.dto.response.UpdateRoleResponse;
import com.tissue.api.workspacemember.validator.WorkspaceMemberValidator;

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
	private final WorkspaceMemberValidator workspaceMemberValidator;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public UpdateNicknameResponse updateDisplayName(
		String workspaceCode,
		Long memberId,
		UpdateDisplayNameRequest request
	) {
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);

		workspaceMember.updateDisplayName(request.displayName());
		workspaceMemberRepository.saveAndFlush(workspaceMember);

		return UpdateNicknameResponse.from(workspaceMember);
	}

	@Transactional
	public UpdateRoleResponse updateRole(
		String workspaceCode,
		Long targetMemberId,
		Long requesterMemberId,
		UpdateRoleRequest request
	) {
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterMemberId, workspaceCode);
		WorkspaceMember target = workspaceMemberReader.findWorkspaceMember(targetMemberId, workspaceCode);

		workspaceMemberValidator.validateRoleUpdate(requester, target);

		WorkspaceRole oldRole = target.getRole();

		target.updateRole(request.updateWorkspaceRole());

		eventPublisher.publishEvent(
			WorkspaceMemberRoleChangedEvent.createEvent(target, oldRole, requesterMemberId)
		);

		return UpdateRoleResponse.from(target);
	}

	@Transactional
	public AssignPositionResponse setPosition(
		String workspaceCode,
		Long positionId,
		Long targetMemberId,
		Long loginMemberId
	) {
		Position position = positionReader.findPosition(positionId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(targetMemberId, workspaceCode);

		workspaceMember.addPosition(position);

		return AssignPositionResponse.from(workspaceMember);
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
	public AssignTeamResponse setTeam(
		String workspaceCode,
		Long teamId,
		Long targetMemberId,
		Long loginMemberId
	) {
		Team team = teamReader.findTeam(teamId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(targetMemberId, workspaceCode);

		workspaceMember.addTeam(team);

		return AssignTeamResponse.from(workspaceMember);
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
	public RemoveWorkspaceMemberResponse removeWorkspaceMember(
		String workspaceCode,
		Long targetMemberId,
		Long requesterMemberId
	) {
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterMemberId, workspaceCode);
		WorkspaceMember target = workspaceMemberReader.findWorkspaceMember(targetMemberId, workspaceCode);

		// TODO: WorkspaceMemberAuthorizationService에 로직 정의해서 사용
		workspaceMemberValidator.validateRemoveMember(requester, target);

		target.remove();

		return RemoveWorkspaceMemberResponse.from(target);
	}
}
