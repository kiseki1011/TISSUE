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
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.KickOutMemberRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.TransferWorkspaceOwnershipRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.UpdateWorkspaceMemberRoleRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.KickOutMemberResponse;
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
	public KickOutMemberResponse kickOutMember(
		String code,
		KickOutMemberRequest request,
		Long requesterId
	) {
		/*
		 * Todo
		 *  - KickOutMemberResponse에서 memberIdentifier를 응답으로 주는게 필요한건지 한번 고민
		 *  - 내 생각에는 굳이 없어도 될듯
		 */
		String memberIdentifier = request.getMemberIdentifier();
		Member member = memberRepository.findByMemberIdentifier(memberIdentifier)
			.orElseThrow(MemberNotFoundException::new);

		WorkspaceMember target = workspaceMemberRepository.findByMemberIdAndWorkspaceCode(member.getId(), code)
			.orElseThrow(MemberNotInWorkspaceException::new);

		WorkspaceMember requester = workspaceMemberRepository
			.findByMemberIdAndWorkspaceCode(requesterId, code)
			.orElseThrow(MemberNotInWorkspaceException::new);

		workspaceMemberValidator.validateKickOutMember(requester, target);

		target.remove();
		workspaceMemberRepository.delete(target);

		return KickOutMemberResponse.from(memberIdentifier, target);
	}

	@Transactional
	public UpdateWorkspaceMemberRoleResponse updateWorkspaceMemberRole(
		String code,
		UpdateWorkspaceMemberRoleRequest request,
		Long requesterId
	) {

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
	public TransferWorkspaceOwnershipResponse transferWorkspaceOwnership(
		String code,
		TransferWorkspaceOwnershipRequest request,
		Long requesterId
	) {

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
