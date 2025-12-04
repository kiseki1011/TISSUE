package com.tissue.api.workspace.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.WorkspaceMember;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;

public interface WorkspaceMemberQueryRepository extends Repository<WorkspaceMember, Long> {

	Optional<WorkspaceMember> findByMember_IdAndWorkspaceKey(
		Long memberId,
		String workspaceKey
	);

	Optional<WorkspaceMember> findByMember_IdAndWorkspace(
		Long memberId,
		Workspace workspace
	);

	Optional<WorkspaceMember> findByMemberAndWorkspace(
		Member member,
		Workspace workspace
	);

	/**
	 * Retrieves a workspace member regardless of their delete status (active or soft-deleted).
	 * <p>
	 * Uses a <b>native query</b> to bypass Hibernate {@link org.hibernate.annotations.SQLRestriction @SqlRestriction}
	 */
	@Query(value = """
		SELECT * FROM workspace_member
		WHERE workspace_key = :workspaceKey
		  AND member_id = :memberId
		  """, nativeQuery = true)
	Optional<WorkspaceMember> findAnyByMemberIdAndWorkspaceKey(
		@Param("memberId") Long memberId,
		@Param("workspaceKey") String workspaceKey
	);

	List<WorkspaceMember> findAllByWorkspace_Key(String workspaceKey);

	@Query("SELECT wm FROM WorkspaceMember wm "
		+ "WHERE wm.workspace.key = :workspaceKey AND wm.role "
		+ "IN ('ADMIN', 'OWNER')")
	Set<WorkspaceMember> findAdminsByWorkspace_Key(@Param("workspaceKey") String workspaceKey);

	List<WorkspaceMember> findAllByMember_IdInAndWorkspaceKey(
		Collection<Long> memberIds,
		String workspaceKey
	);

	@Query("SELECT wm.member.id FROM WorkspaceMember wm " +
		"WHERE wm.workspaceKey = :workspaceKey " +
		"AND wm.member.id IN :candidateIds " +
		"AND wm.softDeleted = false")
	Set<Long> findJoinedMemberIds(
		@Param("workspaceKey") String workspaceKey,
		@Param("candidateIds") Collection<Long> candidateIds
	);

	boolean existsByMemberAndRole(Member member, WorkspaceRole role);

	boolean existsByMemberAndWorkspace(Member member, Workspace workspace);

	long countByWorkspaceKey(String workspaceKey);

	long countByMemberAndRole(Member member, WorkspaceRole role);

	long countByMember(Member member);
}
