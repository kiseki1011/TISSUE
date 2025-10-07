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

	Page<WorkspaceMember> findByMember_Id(Long memberId, Pageable pageable);

	Optional<WorkspaceMember> findByMember_IdAndWorkspace_Key(Long memberId, String workspaceKey);

	boolean existsByMember_IdAndRole(Long memberId, WorkspaceRole role);

	boolean existsByMember_IdAndWorkspace_Key(Long memberId, String workspaceKey);

	List<WorkspaceMember> findAllByWorkspace_KeyAndMember_IdIn(String workspaceKey, Set<Long> memberIdList);

	List<WorkspaceMember> findAllByWorkspace_Key(String workspaceKey);

	@Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace.key = :workspaceKey AND wm.role IN ('ADMIN', 'OWNER')")
	Set<WorkspaceMember> findAdminsByWorkspace_Key(@Param("workspaceKey") String workspaceKey);

	@Query("SELECT wm FROM WorkspaceMember wm "
		+ "WHERE (wm.member.loginId = :identifier OR wm.member.email = :identifier) "
		+ "AND wm.workspace.key = :workspaceKey")
	Optional<WorkspaceMember> findByMemberIdentifierAndWorkspaceKey(
		@Param("identifier") String identifier,
		@Param("workspaceKey") String workspaceKey
	);
}
