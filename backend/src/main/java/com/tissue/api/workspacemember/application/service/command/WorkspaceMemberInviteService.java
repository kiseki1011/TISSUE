package com.tissue.api.workspacemember.application.service.command;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.infrastructure.repository.InvitationRepository;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.application.service.command.WorkspaceReader;
import com.tissue.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.tissue.api.workspacemember.presentation.dto.response.InviteMembersResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberInviteService {

	private final WorkspaceReader workspaceReader;
	private final MemberRepository memberRepository;
	private final InvitationRepository invitationRepository;

	@Transactional
	public InviteMembersResponse inviteMembers(
		String workspaceCode,
		InviteMembersRequest request
	) {

		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);

		// 초대 가능한 멤버 필터링
		List<Member> members = filterInvitableMembers(workspaceCode, request.memberIdentifiers());

		// 초대장 생성
		members.forEach(member -> createInvitation(workspace, member));

		if (members.isEmpty()) {
			throw new InvalidOperationException("No members were available for invitation.");
		}

		return InviteMembersResponse.from(workspaceCode, members);
	}

	private List<Member> filterInvitableMembers(String workspaceCode, Set<String> memberIdentifiers) {
		// 이미 참여중이거나 초대중인 멤버 ID 조회
		Set<Long> existingMemberIds = invitationRepository.findExistingMemberIds(workspaceCode);

		return memberRepository.findAllByEmailInOrLoginIdIn(memberIdentifiers).stream()
			.filter(member -> !existingMemberIds.contains(member.getId()))
			.toList();
	}

	private void createInvitation(Workspace workspace, Member member) {
		Invitation invitation = Invitation.createPendingInvitation(workspace, member);
		invitationRepository.save(invitation);
	}
}
