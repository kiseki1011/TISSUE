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
import com.tissue.api.workspacemember.exception.MemberNotInWorkspaceException;
import com.tissue.api.workspacemember.exception.DuplicateNicknameException;
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
		String code,
		Long memberId,
		UpdateNicknameRequest request
	) {
		try {
			WorkspaceMember workspaceMember = findWorkspaceMember(code, memberId);

			workspaceMember.updateNickname(request.getNickname());
			workspaceMemberRepository.saveAndFlush(workspaceMember);

			return UpdateNicknameResponse.from(workspaceMember);

		} catch (DataIntegrityViolationException | ConstraintViolationException e) {
			log.error("Exception: ", e);
			throw new DuplicateNicknameException(e);
		}
	}

	@Transactional
	public UpdateRoleResponse updateWorkspaceMemberRole(
		String code,
		Long targetId,
		Long requesterId,
		UpdateRoleRequest request
	) {
		WorkspaceMember requester = findWorkspaceMember(code, requesterId);
		WorkspaceMember target = findWorkspaceMember(code, targetId);

		workspaceMemberValidator.validateRoleUpdate(requester, target);

		target.updateRole(request.getUpdateWorkspaceRole());

		return UpdateRoleResponse.from(target);
	}

	@Transactional
	public AssignPositionResponse assignPosition(
		String code,
		Long positionId,
		Long memberId
	) {
		Position position = findPosition(positionId);

		WorkspaceMember workspaceMember = findWorkspaceMember(code, memberId);
		workspaceMember.changePosition(position);

		return AssignPositionResponse.from(workspaceMember);
	}

	@Transactional
	public void removePosition(
		String code,
		Long memberId
	) {
		WorkspaceMember workspaceMember = findWorkspaceMember(code, memberId);
		workspaceMember.removePosition();
	}

	@Transactional
	public TransferOwnershipResponse transferWorkspaceOwnership(
		String code,
		Long targetId,
		Long requesterId
	) {
		WorkspaceMember requester = findWorkspaceMember(code, requesterId);
		WorkspaceMember target = findWorkspaceMember(code, targetId);

		requester.updateRoleFromOwnerToAdmin();
		target.updateRoleToOwner();

		return TransferOwnershipResponse.from(requester, target);
	}

	@Transactional
	public RemoveWorkspaceMemberResponse removeWorkspaceMember(
		String code,
		Long targetId,
		Long requesterId
	) {
		WorkspaceMember requester = findWorkspaceMember(code, requesterId);
		WorkspaceMember target = findWorkspaceMember(code, targetId);

		workspaceMemberValidator.validateRemoveMember(requester, target);

		target.remove();
		workspaceMemberRepository.delete(target);

		return RemoveWorkspaceMemberResponse.from(targetId, target);
	}

	private WorkspaceMember findWorkspaceMember(String code, Long memberId) {
		return workspaceMemberRepository
			.findByMemberIdAndWorkspaceCode(memberId, code)
			.orElseThrow(MemberNotInWorkspaceException::new);
	}

	private Position findPosition(Long positionId) {
		return positionRepository
			.findById(positionId)
			.orElseThrow(PositionNotFoundException::new);
	}
}
