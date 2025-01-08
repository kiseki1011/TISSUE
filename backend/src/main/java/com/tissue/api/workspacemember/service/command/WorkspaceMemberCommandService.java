package com.tissue.api.workspacemember.service.command;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.position.domain.Position;
import com.tissue.api.position.domain.repository.PositionRepository;
import com.tissue.api.position.exception.PositionNotFoundException;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.DuplicateNicknameException;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateNicknameRequest;
import com.tissue.api.workspacemember.presentation.dto.request.UpdateRoleRequest;
import com.tissue.api.workspacemember.presentation.dto.response.AssignPositionResponse;
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
			log.error("Exception: ", e);
			throw new DuplicateNicknameException(e);
		}
	}

	@Transactional
	public UpdateRoleResponse updateWorkspaceMemberRole(
		Long targetId,
		Long requesterId,
		UpdateRoleRequest request
	) {
		WorkspaceMember requester = findWorkspaceMember(requesterId);
		WorkspaceMember target = findWorkspaceMember(targetId);

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
	public TransferOwnershipResponse transferWorkspaceOwnership(
		Long targetId,
		Long requesterId
	) {
		WorkspaceMember requester = findWorkspaceMember(requesterId);
		WorkspaceMember target = findWorkspaceMember(targetId);

		requester.updateRoleFromOwnerToAdmin();
		target.updateRoleToOwner();

		return TransferOwnershipResponse.from(requester, target);
	}

	@Transactional
	public RemoveWorkspaceMemberResponse removeWorkspaceMember(
		Long targetId,
		Long requesterId
	) {
		WorkspaceMember requester = findWorkspaceMember(requesterId);
		WorkspaceMember target = findWorkspaceMember(targetId);

		workspaceMemberValidator.validateRemoveMember(requester, target);

		target.remove();
		workspaceMemberRepository.delete(target);

		return RemoveWorkspaceMemberResponse.from(target);
	}

	private WorkspaceMember findWorkspaceMember(Long workspaceMemberId) {
		return workspaceMemberRepository.findById(workspaceMemberId)
			.orElseThrow(WorkspaceMemberNotFoundException::new);
	}

	private Position findPosition(Long positionId, String workspaceCode) {
		return positionRepository
			.findByIdAndWorkspaceCode(positionId, workspaceCode)
			.orElseThrow(PositionNotFoundException::new);
	}
}
