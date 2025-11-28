package com.tissue.api.project.domain.port.out;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.ProjectMember;
import com.tissue.api.project.domain.enums.ProjectRole;
import com.tissue.api.workspace.domain.WorkspaceMember;

public interface ProjectMemberQueryRepository extends Repository<ProjectMember, Long> {

	Optional<ProjectMember> findByWorkspaceMemberAndProject(
		WorkspaceMember workspaceMember,
		Project project
	);

	Optional<ProjectMember> findByWorkspaceKeyAndProjectKeyAndMemberId(
		String workspaceKey,
		String projectKey,
		Long memberId
	);

	Optional<ProjectMember> findByProjectAndMemberId(
		Project project,
		Long memberId
	);

	/**
	 * Retrieves a project member regardless of their delete status (active or soft-deleted).
	 * <p>
	 * This method uses a <b>native query</b> to bypass the Hibernate {@code @SQLRestriction}.
	 */
	@Query(value = """
		SELECT * FROM project_member
		WHERE project_id = :projectId
		  AND member_id = :memberId
		""", nativeQuery = true)
	Optional<ProjectMember> findAnyByProjectIdAndMemberId(
		@Param("projectId") Long projectId,
		@Param("memberId") Long memberId
	);

	@Query("SELECT pm.role FROM ProjectMember pm " +
		"WHERE pm.workspaceKey = :workspaceKey " +
		"AND pm.projectKey = :projectKey " +
		"AND pm.memberId = :memberId")
	Optional<ProjectRole> findRoleByKeysAndMemberId(
		@Param("workspaceKey") String workspaceKey,
		@Param("projectKey") String projectKey,
		@Param("memberId") Long memberId
	);

	boolean existsByWorkspaceKeyAndProjectKeyAndMemberId(
		String workspaceKey,
		String projectKey,
		Long memberId
	);

	boolean existsByProjectAndMemberId(
		Project project,
		Long memberId
	);

	@Query("""
		    SELECT pm.memberId
		    FROM ProjectMember pm
		    WHERE pm.project = :project
		      AND pm.memberId IN :memberIds
		      AND pm.softDeleted = false
		""")
	Set<Long> findMemberIdsByProjectAndMemberIds(
		@Param("project") Project project,
		@Param("memberIds") Collection<Long> memberIds
	);
}
