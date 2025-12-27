package com.tissue.workspace.application.service.finder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.member.domain.Member;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.WorkspaceExceptions;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceMemberFinder {

	private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

	public Optional<WorkspaceMember> findAnyOptionalBy(Long memberId, String workspaceKey) {
		return workspaceMemberQueryRepository.findAnyByMemberIdAndWorkspaceKey(memberId, workspaceKey);
	}

	public WorkspaceMember findBy(Long memberId, String workspaceKey) {
		return workspaceMemberQueryRepository.findByMember_IdAndWorkspaceKey(memberId, workspaceKey)
			.orElseThrow(() -> WorkspaceExceptions.memberNotFound(memberId, workspaceKey));
	}

	public WorkspaceMember findBy(Long memberId, Workspace workspace) {
		return workspaceMemberQueryRepository.findByMember_IdAndWorkspace(memberId, workspace)
			.orElseThrow(() -> WorkspaceExceptions.memberNotFound(memberId, workspace.getKey()));
	}

	public Optional<WorkspaceMember> findOptionalBy(Member member, Workspace workspace) {
		return workspaceMemberQueryRepository.findByMemberAndWorkspace(member, workspace);
	}

	public boolean existsBy(Member member, Workspace workspace) {
		return workspaceMemberQueryRepository.existsByMemberAndWorkspace(member, workspace);
	}

	public List<WorkspaceMember> findAllBy(Collection<Long> memberIds, String workspaceKey) {
		return workspaceMemberQueryRepository.findAllByMember_IdInAndWorkspaceKey(memberIds, workspaceKey);
	}

	public Set<Long> findJoinedMemberIdsBy(String workspaceKey, Collection<Long> memberIds) {
		return workspaceMemberQueryRepository.findJoinedMemberIds(workspaceKey, memberIds);
	}

	public int countTotalMembersBy(String workspaceKey) {
		return (int)workspaceMemberQueryRepository.countByWorkspaceKey(workspaceKey);
	}

	public int countOwnedWorkspacesBy(Member member) {
		return (int)workspaceMemberQueryRepository.countByMemberAndRole(member, WorkspaceRole.OWNER);
	}

	public int countJoinedWorkspacesBy(Member member) {
		return (int)workspaceMemberQueryRepository.countByMember(member);
	}
}
