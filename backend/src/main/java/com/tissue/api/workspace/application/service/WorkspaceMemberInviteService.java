package com.tissue.api.workspace.application.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.invitation.domain.model.Invitation;
import com.tissue.api.invitation.infrastructure.repository.InvitationRepository;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.adapter.in.web.dto.request.InviteMembersRequest;
import com.tissue.api.workspace.adapter.in.web.dto.response.InviteMembersResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberInviteService {

	private final WorkspaceFinder workspaceFinder;
	private final MemberRepository memberRepository;
	private final InvitationRepository invitationRepository;

	@Transactional
	public InviteMembersResponse inviteMembers(
		String workspaceKey,
		InviteMembersRequest request
	) {
		Workspace workspace = workspaceFinder.findWorkspace(workspaceKey);

		// 초대 가능한 멤버 필터링
		List<Member> members = filterInvitableMembers(workspaceKey, request.emails());

		// 초대장 생성
		members.forEach(member -> createInvitation(workspace, member));

		if (members.isEmpty()) {
			// TODO: 예외 던지지 말고 대상이 없다면 그냥 무시하기?
			// throw new RuntimeException("No members were available for invitation.");
		}

		return InviteMembersResponse.from(workspaceKey, members);
	}

	private List<Member> filterInvitableMembers(String workspaceKey, Set<String> emails) {
		// 이미 참여중이거나 초대중인 멤버 ID 조회
		Set<Long> existingMemberIds = invitationRepository.findExistingMemberIds(workspaceKey);

		return memberRepository.findAllByEmailIn(emails).stream()
			.filter(member -> !existingMemberIds.contains(member.getId()))
			.toList();
	}

	private void createInvitation(Workspace workspace, Member member) {
		Invitation invitation = Invitation.createPendingInvitation(workspace, member);
		invitationRepository.save(invitation);
	}
}
