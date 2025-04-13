package com.tissue.api.workspacemember.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

	Optional<WorkspaceMember> findByIdAndWorkspaceCode(Long workspaceMemberId, String workspaceCode);

	boolean existsByMemberIdAndRole(Long id, WorkspaceRole role);

	boolean existsByMemberIdAndWorkspaceCode(Long id, String workspaceCode);

	List<WorkspaceMember> findAllByIdIn(Set<Long> idList);

	List<WorkspaceMember> findAllByWorkspaceCode(String workspaceCode);

	// WorspaceRole에 Comparable을 구현하는 방식은 불가능함!(level 비교 불가)
	@Deprecated
	Set<WorkspaceMember> findAllByWorkspaceCodeAndRoleGreaterThanEqual(
		String workspaceCode,
		WorkspaceRole role
	);

	@Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspaceCode = :workspaceCode AND wm.role IN ('ADMIN', 'OWNER')")
	Set<WorkspaceMember> findAdminsByWorkspaceCode(@Param("workspaceCode") String workspaceCode);

	Optional<WorkspaceMember> findByWorkspaceCodeAndId(
		String workspaceCode,
		Long workspaceMemberId
	);

	@Query("SELECT wm FROM WorkspaceMember wm "
		+ "WHERE (wm.member.loginId = :identifier OR wm.member.email = :identifier) "
		+ "AND wm.workspace.code = :workspaceCode")
	Optional<WorkspaceMember> findByMemberIdentifierAndWorkspaceCode(
		@Param("identifier") String identifier,
		@Param("workspaceCode") String workspaceCode
	);
}
