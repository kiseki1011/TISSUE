package com.tissue.api.workspacemember.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

	Page<WorkspaceMember> findByMemberId(Long memberId, Pageable pageable);

	Optional<WorkspaceMember> findByMemberIdAndWorkspaceCode(Long memberId, String workspaceCode);

	Optional<WorkspaceMember> findByMemberIdAndWorkspaceId(Long memberId, Long workspaceId);

	boolean existsByMemberIdAndRole(Long id, WorkspaceRole role);

	boolean existsByMemberIdAndWorkspaceCode(Long id, String workspaceCode);

	@Query("SELECT wm FROM WorkspaceMember wm "
		+ "WHERE (wm.member.loginId = :identifier OR wm.member.email = :identifier) "
		+ "AND wm.workspace.code = :workspaceCode")
	Optional<WorkspaceMember> findByMemberIdentifierAndWorkspaceCode(
		@Param("identifier") String identifier,
		@Param("workspaceCode") String workspaceCode
	);
}
