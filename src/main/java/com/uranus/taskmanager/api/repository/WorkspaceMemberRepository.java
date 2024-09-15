package com.uranus.taskmanager.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uranus.taskmanager.api.domain.member.Member;
import com.uranus.taskmanager.api.domain.workspace.Workspace;
import com.uranus.taskmanager.api.domain.workspaceuser.WorkspaceMember;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
	List<WorkspaceMember> findByWorkspaceId(Long workspaceId);

	Optional<WorkspaceMember> findByWorkspaceCodeAndUserId(Long workspaceId, Long userId);

	boolean existsByMemberAndWorkspace(Member member, Workspace workspace);
}
