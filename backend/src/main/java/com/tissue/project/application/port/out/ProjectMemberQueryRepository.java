package com.tissue.project.application.port.out;

import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberQueryRepository extends Repository<ProjectMember, Long> {

    // TODO: WorkspaceMember와 같이 조회(JOIN FETCH)

    /**
     * Retrieves a project member regardless of their delete status (active or soft-deleted).
     *
     * <p>Uses a <b>native query</b> to bypass Hibernate {@link
     * org.hibernate.annotations.SQLRestriction @SqlRestriction}
     */
    @Query(
            value =
                    """
                    SELECT * FROM project_member
                    WHERE project_id = :projectId
                      AND member_id = :memberId
                    """,
            nativeQuery = true)
    Optional<ProjectMember> findAnyByProjectIdAndMemberId(
            @Param("projectId") Long projectId, @Param("memberId") Long memberId);

    @Query(
            "SELECT pm.role FROM ProjectMember pm "
                    + "WHERE pm.workspaceKey = :workspaceKey "
                    + "AND pm.projectKey = :projectKey "
                    + "AND pm.memberId = :memberId")
    Optional<ProjectRole> findRoleByKeysAndMemberId(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("memberId") Long memberId);

    @Query(
            """
                SELECT pm.memberId
                FROM ProjectMember pm
                WHERE pm.project = :project
                  AND pm.memberId IN :memberIds
                  AND pm.softDeleted = false
            """)
    Set<Long> findMemberIdsByProjectAndMemberIds(
            @Param("project") Project project, @Param("memberIds") Collection<Long> memberIds);

    @Query(
            """
                SELECT pm
                FROM ProjectMember pm
                WHERE pm.workspaceKey = :workspaceKey
                  AND pm.memberId = :memberId
                  AND pm.projectKey IN :projectKeys
                  AND pm.role = 'ADMIN'
            """)
    List<ProjectMember> findAllAdminsByKeysAndMemberId(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKeys") Collection<String> projectKeys,
            @Param("memberId") Long memberId);

    boolean existsByWorkspaceKeyAndProjectKeyAndMemberId(
            String workspaceKey, String projectKey, Long memberId);

    boolean existsByProjectAndMemberId(Project project, Long memberId);

    List<ProjectMember> findAllByMemberId(Long memberId);
}
