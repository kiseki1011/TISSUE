package com.tissue.api.workspacemember.service.command;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.position.domain.Position;
import com.tissue.api.position.domain.repository.PositionRepository;
import com.tissue.api.team.domain.Team;
import com.tissue.api.team.domain.repository.TeamRepository;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;
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

	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceMemberValidator workspaceMemberValidator;
	private final PositionRepository positionRepository;
	private final TeamRepository teamRepository;

	@Transactional
	public UpdateNicknameResponse updateNickname(
		Long workspaceMemberId,
		UpdateNicknameRequest request
	) {
		try {
			WorkspaceMember workspaceMember = findWorkspaceMember(workspaceMemberId);

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
		WorkspaceMember requester = findWorkspaceMember(requesterWorkspaceMemberId);
		WorkspaceMember target = findWorkspaceMember(targetWorkspaceMemberId);

		workspaceMemberValidator.validateRoleUpdate(requester, target);

		target.updateRole(request.updateWorkspaceRole());

		return UpdateRoleResponse.from(target);
	}

	@Transactional
	public AssignPositionResponse assignPosition(
		String workspaceCode,
		Long positionId,
		Long workspaceMemberId
	) {
		Position position = findPosition(positionId, workspaceCode);
		WorkspaceMember workspaceMember = findWorkspaceMember(workspaceMemberId);

		workspaceMember.addPosition(position);

		return AssignPositionResponse.from(workspaceMember);
	}

	@Transactional
	public void removePosition(
		String workspaceCode,
		Long positionId,
		Long workspaceMemberId
	) {
		Position position = findPosition(positionId, workspaceCode);
		WorkspaceMember workspaceMember = findWorkspaceMember(workspaceMemberId);

		workspaceMember.removePosition(position);
	}

	@Transactional
	public AssignTeamResponse assignTeam(
		String workspaceCode,
		Long teamId,
		Long workspaceMemberId
	) {
		Team team = findTeam(teamId, workspaceCode);
		WorkspaceMember workspaceMember = findWorkspaceMember(workspaceMemberId);

		workspaceMember.addTeam(team);

		return AssignTeamResponse.from(workspaceMember);
	}

	@Transactional
	public void removeTeam(
		String workspaceCode,
		Long teamId,
		Long workspaceMemberId
	) {
		Team team = findTeam(teamId, workspaceCode);
		WorkspaceMember workspaceMember = findWorkspaceMember(workspaceMemberId);

		workspaceMember.removeTeam(team);
	}

	@Transactional
	public TransferOwnershipResponse transferWorkspaceOwnership(
		Long targetWorkspaceMemberId,
		Long requesterWorkspaceMemberId
	) {
		WorkspaceMember requester = findWorkspaceMember(requesterWorkspaceMemberId);
		WorkspaceMember target = findWorkspaceMember(targetWorkspaceMemberId);

		requester.updateRoleFromOwnerToAdmin();
		target.updateRoleToOwner();

		return TransferOwnershipResponse.from(requester, target);
	}

	@Transactional
	public RemoveWorkspaceMemberResponse removeWorkspaceMember(
		Long targetWorkspaceMemberId,
		Long requesterWorkspaceMemberId
	) {
		WorkspaceMember requester = findWorkspaceMember(requesterWorkspaceMemberId);
		WorkspaceMember target = findWorkspaceMember(targetWorkspaceMemberId);

		workspaceMemberValidator.validateRemoveMember(requester, target);

		target.remove();
		workspaceMemberRepository.delete(target);

		return RemoveWorkspaceMemberResponse.from(target);
	}

	private WorkspaceMember findWorkspaceMember(Long id) {
		return workspaceMemberRepository.findById(id)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(id));
	}

	private Position findPosition(Long positionId, String workspaceCode) {
		return positionRepository
			.findByIdAndWorkspaceCode(positionId, workspaceCode)
			.orElseThrow(() -> new ResourceNotFoundException(
				String.format("Position was not found with positionId: %d, workspaceCode: %s",
					positionId, workspaceCode)));
	}

	private Team findTeam(Long teamId, String workspaceCode) {
		return teamRepository
			.findByIdAndWorkspaceCode(teamId, workspaceCode)
			.orElseThrow(() -> new ResourceNotFoundException(
				String.format("Team was not found with teamId: %d, workspaceCode: %s",
					teamId, workspaceCode)));
	}
}
