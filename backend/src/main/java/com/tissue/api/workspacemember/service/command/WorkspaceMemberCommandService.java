package com.tissue.api.workspacemember.service.command;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.position.domain.Position;
import com.tissue.api.position.service.command.PositionReader;
import com.tissue.api.team.domain.Team;
import com.tissue.api.team.service.command.TeamReader;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.domain.event.WorkspaceMemberRoleChangedEvent;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateNicknameRequest;
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
	public UpdateNicknameResponse updateNickname(
		Long workspaceMemberId,
		UpdateNicknameRequest request
	) {
		try {
			WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(workspaceMemberId);

			workspaceMember.updateNickname(request.nickname());
			workspaceMemberRepository.saveAndFlush(workspaceMember);

			return UpdateNicknameResponse.from(workspaceMember);

		} catch (DataIntegrityViolationException | ConstraintViolationException e) {
			log.error("Duplicate nickname: ", e);

			throw new DuplicateResourceException(
				String.format("Nickname already exists for this workspace. nickname: %s", request.nickname()), e);
		}
	}

	@Transactional
	public UpdateRoleResponse updateWorkspaceMemberRole(
		Long targetWorkspaceMemberId,
		Long requesterWorkspaceMemberId,
		UpdateRoleRequest request
	) {
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);
		WorkspaceMember target = workspaceMemberReader.findWorkspaceMember(targetWorkspaceMemberId);

		workspaceMemberValidator.validateRoleUpdate(requester, target);

		WorkspaceRole oldRole = target.getRole();

		target.updateRole(request.updateWorkspaceRole());

		eventPublisher.publishEvent(
			WorkspaceMemberRoleChangedEvent.createEvent(target, oldRole, requesterWorkspaceMemberId)
		);

		return UpdateRoleResponse.from(target);
	}

	@Transactional
	public AssignPositionResponse assignPosition(
		String workspaceCode,
		Long positionId,
		Long workspaceMemberId
	) {
		Position position = positionReader.findPosition(positionId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(workspaceMemberId);

		workspaceMember.addPosition(position);

		return AssignPositionResponse.from(workspaceMember);
	}

	@Transactional
	public void removePosition(
		String workspaceCode,
		Long positionId,
		Long workspaceMemberId
	) {
		Position position = positionReader.findPosition(positionId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(workspaceMemberId);

		workspaceMember.removePosition(position);
	}

	@Transactional
	public AssignTeamResponse assignTeam(
		String workspaceCode,
		Long teamId,
		Long workspaceMemberId
	) {
		Team team = teamReader.findTeam(teamId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(workspaceMemberId);

		workspaceMember.addTeam(team);

		return AssignTeamResponse.from(workspaceMember);
	}

	@Transactional
	public void removeTeam(
		String workspaceCode,
		Long teamId,
		Long workspaceMemberId
	) {
		Team team = teamReader.findTeam(teamId, workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(workspaceMemberId);

		workspaceMember.removeTeam(team);
	}

	@Transactional
	public TransferOwnershipResponse transferWorkspaceOwnership(
		Long targetWorkspaceMemberId,
		Long requesterWorkspaceMemberId
	) {
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);
		WorkspaceMember target = workspaceMemberReader.findWorkspaceMember(targetWorkspaceMemberId);

		requester.updateRoleFromOwnerToAdmin();
		target.updateRoleToOwner();

		return TransferOwnershipResponse.from(requester, target);
	}

	@Transactional
	public RemoveWorkspaceMemberResponse removeWorkspaceMember(
		Long targetWorkspaceMemberId,
		Long requesterWorkspaceMemberId
	) {
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);
		WorkspaceMember target = workspaceMemberReader.findWorkspaceMember(targetWorkspaceMemberId);

		workspaceMemberValidator.validateRemoveMember(requester, target);

		target.remove();

		return RemoveWorkspaceMemberResponse.from(target);
	}
}
