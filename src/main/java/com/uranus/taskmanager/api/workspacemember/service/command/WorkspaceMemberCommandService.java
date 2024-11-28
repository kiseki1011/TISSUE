package com.uranus.taskmanager.api.workspacemember.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.api.workspacemember.exception.MemberNotInWorkspaceException;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.KickWorkspaceMemberRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.TransferWorkspaceOwnershipRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateWorkspaceMemberRoleRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.KickWorkspaceMemberResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.TransferWorkspaceOwnershipResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.UpdateWorkspaceMemberRoleResponse;
import com.uranus.taskmanager.api.workspacemember.validator.WorkspaceMemberValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberCommandService {

	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceMemberValidator workspaceMemberValidator;

	@Transactional
	public KickWorkspaceMemberResponse kickWorkspaceMember(String code, KickWorkspaceMemberRequest request) {

		Workspace workspace = workspaceRepository.findByCode(code)
			.orElseThrow(WorkspaceNotFoundException::new);

		String identifier = request.getMemberIdentifier();
		Member member = memberRepository.findByMemberIdentifier(identifier)
			.orElseThrow(MemberNotFoundException::new);

		WorkspaceMember workspaceMember = workspaceMemberRepository.findByMemberIdAndWorkspaceCode(member.getId(), code)
			.orElseThrow(MemberNotInWorkspaceException::new);

		workspaceMemberRepository.delete(workspaceMember);
		workspace.decreaseMemberCount();

		return KickWorkspaceMemberResponse.from(identifier, workspaceMember);
	}

	@Transactional
	public UpdateWorkspaceMemberRoleResponse updateWorkspaceMemberRole(String code,
		UpdateWorkspaceMemberRoleRequest request, Long requesterId) {

		WorkspaceMember requester = workspaceMemberRepository
			.findByMemberIdAndWorkspaceCode(requesterId, code)
			.orElseThrow(MemberNotInWorkspaceException::new);

		WorkspaceMember target = workspaceMemberRepository
			.findByMemberIdentifierAndWorkspaceCode(request.getMemberIdentifier(), code)
			.orElseThrow(MemberNotInWorkspaceException::new);

		workspaceMemberValidator.validateRoleUpdate(requester, target);

		target.updateRole(request.getUpdateWorkspaceRole());

		return UpdateWorkspaceMemberRoleResponse.from(target);
	}

	@Transactional
	public TransferWorkspaceOwnershipResponse transferWorkspaceOwnership(String code,
		TransferWorkspaceOwnershipRequest request, Long requesterId) {

		Workspace workspace = workspaceRepository.findByCode(code)
			.orElseThrow(WorkspaceNotFoundException::new);

		WorkspaceMember requester = workspaceMemberRepository
			.findByMemberIdAndWorkspaceId(requesterId, workspace.getId())
			.orElseThrow(MemberNotInWorkspaceException::new);

		WorkspaceMember target = workspaceMemberRepository.findByMemberIdentifierAndWorkspaceCode(
				request.getMemberIdentifier(), code)
			.orElseThrow(MemberNotInWorkspaceException::new);

		requester.updateRoleFromOwnerToManager();
		target.updateRoleToOwner();

		return TransferWorkspaceOwnershipResponse.from(target);
	}
}
