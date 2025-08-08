package com.tissue.api.workspacemember.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

	Page<WorkspaceMember> findByMemberId(Long memberId, Pageable pageable);

	Optional<WorkspaceMember> findByMemberIdAndWorkspaceKey(Long memberId, String workspaceKey);

	Optional<WorkspaceMember> findByMemberIdAndWorkspaceId(Long memberId, Long workspaceId);

	Optional<WorkspaceMember> findByIdAndWorkspaceKey(Long workspaceMemberId, String workspaceKey);

	boolean existsByMemberIdAndRole(Long id, WorkspaceRole role);

	boolean existsByMemberIdAndWorkspaceKey(Long id, String workspaceKey);

	List<WorkspaceMember> findAllByWorkspaceKeyAndMemberIdIn(String workspaceKey, Set<Long> memberIdList);

	List<WorkspaceMember> findAllByWorkspaceKey(String workspaceKey);

	@Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace.key = :workspaceKey AND wm.role IN ('ADMIN', 'OWNER')")
	Set<WorkspaceMember> findAdminsByWorkspaceKey(@Param("workspaceKey") String workspaceKey);

	@Query("SELECT wm FROM WorkspaceMember wm "
		+ "WHERE (wm.member.loginId = :identifier OR wm.member.email = :identifier) "
		+ "AND wm.workspace.key = :workspaceKey")
	Optional<WorkspaceMember> findByMemberIdentifierAndWorkspaceKey(
		@Param("identifier") String identifier,
		@Param("workspaceKey") String workspaceKey
	);
}
