package com.uranus.taskmanager.api.workspace.workspacemember.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.workspacemember.domain.WorkspaceMember;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
	List<WorkspaceMember> findByWorkspaceId(Long workspaceId);

	boolean existsByMemberAndWorkspace(Member member, Workspace workspace);
}
