package com.tissue.api.workspacemember.application.service.command;

import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.application.service.command.MemberFinder;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspace.domain.policy.WorkspacePolicy;
import com.tissue.api.workspacemember.application.finder.WorkspaceMemberFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.presentation.dto.response.WorkspaceMemberResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceParticipationService {

	private final WorkspaceFinder workspaceFinder;
	private final MemberFinder memberFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspacePolicy workspacePolicy;
	// private final MemberPolicy memberPolicy;

	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public WorkspaceMemberResponse joinWorkspace(String workspaceKey, Long memberId) {

		Workspace workspace = workspaceFinder.findWorkspaceWithMembers(workspaceKey);
		Member member = memberFinder.findMemberById(memberId);

		Optional<WorkspaceMember> existingMemberOpt = workspaceMemberRepository
			.findByMemberAndWorkspace(member, workspace);

		if (existingMemberOpt.isPresent()) {
			return WorkspaceMemberResponse.from(existingMemberOpt.get());
		}

		// TODO: ensureCanJoin은 삭제할까? 아니면 일정 수준을 넘지 못하도록 제한을 둘까? 악의적인 참여 사용을 못하도록
		//  500개 선에서 막는게 좋을 것 같긴함
		// memberPolicy.ensureCanJoin(member);

		// workspacePolicy.ensureCanAddMember(workspace);

		// TODO: 서바스의 파라미터로 WorkspaceRole도 받아서 role을 넘기기
		WorkspaceMember workspaceMember = workspace.addMember(member, WorkspaceRole.MEMBER);

		// TODO: eventPublisher.publishEvent(new MemberJoinedWorkspaceEvent)

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public void leaveWorkspace(String workspaceKey, Long memberId) {

		Workspace workspace = workspaceFinder.findWorkspace(workspaceKey);
		WorkspaceMember workspaceMember = workspaceMemberFinder.findByMemberIdAndWorkspace(memberId, workspace);

		// TODO: workspaceMemberValidator 또는 workspacePolicy 또는 workspaceMemberPolicy 만들어서 사용
		//  예시: workspacePolicy.ensureCanLeaveWorkspace(workspaceMember);
		workspaceMember.validateCanLeaveWorkspace();

		workspace.removeMember(workspaceMember);
	}
}
