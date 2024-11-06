package com.uranus.taskmanager.api.workspacemember.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
	List<WorkspaceMember> findByMemberLoginId(String loginId);

	Page<WorkspaceMember> findByMemberLoginId(String loginId, Pageable pageable);

	Page<WorkspaceMember> findByMemberId(Long id, Pageable pageable);

	Optional<WorkspaceMember> findByMemberLoginIdAndWorkspaceId(String loginId, Long workspaceId);

	Optional<WorkspaceMember> findByMemberLoginIdAndWorkspaceCode(String loginId, String workspaceCode);

	Optional<WorkspaceMember> findByMemberIdAndWorkspaceCode(Long id, String workspaceCode);

	int countByWorkspaceId(Long workspaceId);

	boolean existsByMemberAndWorkspace(Member member, Workspace workspace);
}
