package com.uranus.taskmanager.api.workspacemember.service.command;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.domain.repository.InvitationRepository;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InviteMembersResponse;

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
			.orElseThrow(WorkspaceNotFoundException::new);

		// 1. 초대 가능한 멤버 필터링
		List<Member> membersToInvite = filterInvitableMembers(workspace.getId(), request.getMemberIdentifiers());

		// 2. 초대장 생성 및 초대된 멤버 정보 수집
		List<InviteMembersResponse.InvitedMember> invitedMembers = membersToInvite.stream()
			.map(member -> {
				createInvitation(workspace, member);
				return InviteMembersResponse.InvitedMember.from(member);
			})
			.toList();

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
