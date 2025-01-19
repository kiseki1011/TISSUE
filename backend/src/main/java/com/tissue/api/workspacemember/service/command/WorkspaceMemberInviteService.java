package com.tissue.api.workspacemember.service.command;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.InvalidOperationException;
import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.domain.repository.InvitationRepository;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;
import com.tissue.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.tissue.api.workspacemember.presentation.dto.response.InviteMembersResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberInviteService {

	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final InvitationRepository invitationRepository;

	@Transactional
	public InviteMembersResponse inviteMembers(String workspaceCode, InviteMembersRequest request) {
		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(() -> new WorkspaceNotFoundException(workspaceCode));

		// 1. 초대 가능한 멤버 필터링
		List<Member> membersToInvite = filterInvitableMembers(workspace.getId(), request.memberIdentifiers());

		// 2. 초대장 생성 및 초대된 멤버 정보 수집
		List<InviteMembersResponse.InvitedMember> invitedMembers = membersToInvite.stream()
			.map(member -> {
				createInvitation(workspace, member);
				return InviteMembersResponse.InvitedMember.from(member);
			})
			.toList();

		if (invitedMembers.isEmpty()) {
			throw new InvalidOperationException("No avaliable members were found for invitation.");
		}

		return InviteMembersResponse.of(workspaceCode, invitedMembers);
	}

	private List<Member> filterInvitableMembers(Long workspaceId, Set<String> memberIdentifiers) {
		// 이미 참여중이거나 초대중인 멤버 ID 조회
		Set<Long> existingMemberIds = invitationRepository.findExistingMemberIds(workspaceId);

		return memberRepository.findAllByEmailInOrLoginIdIn(memberIdentifiers).stream()
			.filter(member -> !existingMemberIds.contains(member.getId()))
			.toList();
	}

	private void createInvitation(Workspace workspace, Member member) {
		Invitation invitation = Invitation.createPendingInvitation(workspace, member);
		invitationRepository.save(invitation);
	}
}
