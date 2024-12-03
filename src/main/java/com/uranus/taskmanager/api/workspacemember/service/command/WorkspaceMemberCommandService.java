package com.uranus.taskmanager.api.workspacemember.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.api.workspacemember.exception.MemberNotInWorkspaceException;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateMemberRoleRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.RemoveMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.TransferOwnershipResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.UpdateMemberRoleResponse;
import com.uranus.taskmanager.api.workspacemember.validator.WorkspaceMemberValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberCommandService {

	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceMemberValidator workspaceMemberValidator;

	@Transactional
	public RemoveMemberResponse removeMember(
		String code,
		Long targetId,
		Long requesterId
	) {
		WorkspaceMember requester = findWorkspaceMember(code, requesterId);
		WorkspaceMember target = findWorkspaceMember(code, targetId);

		workspaceMemberValidator.validateRemoveMember(requester, target);

		target.remove();
		workspaceMemberRepository.delete(target);

		return RemoveMemberResponse.from(targetId, target);
	}

	@Transactional
	public UpdateMemberRoleResponse updateWorkspaceMemberRole(
		String code,
		Long targetId,
		Long requesterId,
		UpdateMemberRoleRequest request
	) {
		WorkspaceMember requester = findWorkspaceMember(code, requesterId);
		WorkspaceMember target = findWorkspaceMember(code, targetId);

		workspaceMemberValidator.validateRoleUpdate(requester, target);

		target.updateRole(request.getUpdateWorkspaceRole());

		return UpdateMemberRoleResponse.from(target);
	}

	@Transactional
	public TransferOwnershipResponse transferWorkspaceOwnership(
		String code,
		Long targetId,
		Long requesterId
	) {
		WorkspaceMember requester = findWorkspaceMember(code, requesterId);
		WorkspaceMember target = findWorkspaceMember(code, targetId);

		requester.updateRoleFromOwnerToManager();
		target.updateRoleToOwner();

		return TransferOwnershipResponse.from(requester, target);
	}

	private WorkspaceMember findWorkspaceMember(String code, Long memberId) {
		return workspaceMemberRepository
			.findByMemberIdAndWorkspaceCode(memberId, code)
			.orElseThrow(MemberNotInWorkspaceException::new);
	}
}
