package com.tissue.feature.project.application.port.out;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.application.port.out.WorkspaceMemberContactInfo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// TODO: Needs refactoring
public interface ProjectMemberQueryRepository extends Repository<ProjectMember, Long> {

    boolean existsByProjectAndMemberId(Project project, Long memberId);

    @Query("SELECT pm " + "FROM ProjectMember pm "
            + "JOIN FETCH pm.workspaceMember wm "
            + "WHERE wm.member.email = :email "
            + "AND pm.projectKey = :projectKey "
            + "AND pm.workspaceKey = :workspaceKey "
            + "AND pm.softDeleted = false")
    Optional<ProjectMember> findWithWorkspaceMemberByEmailAndKeys(
            @Param("email") String email,
            @Param("projectKey") String projectKey,
            @Param("workspaceKey") String workspaceKey);

    @Query("SELECT pm " + "FROM ProjectMember pm "
            + "JOIN FETCH pm.workspaceMember wm "
            + "WHERE pm.workspaceKey = :workspaceKey "
            + "AND pm.projectKey = :projectKey "
            + "AND pm.memberId = :memberId "
            + "AND pm.softDeleted = false")
    Optional<ProjectMember> findActiveWithWorkspaceMemberByKeysAndMemberId(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("memberId") Long memberId);

    @Query("""
           SELECT pm
           FROM ProjectMember pm
           JOIN FETCH pm.project p
           WHERE pm.workspaceKey = :workspaceKey
             AND pm.projectKey = :projectKey
             AND pm.memberId = :memberId
       """)
    Optional<ProjectMember> findWithProjectByKeys(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("memberId") Long memberId);

    Optional<ProjectMember> findByProjectAndMemberId(Project project, Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProjectMember pm SET pm.softDeleted = true, pm.softDeletedAt = CURRENT_TIMESTAMP, "
            + "pm.archived = true, pm.archivedAt = CURRENT_TIMESTAMP "
            + "WHERE pm.workspaceKey = :workspaceKey AND pm.memberId = :memberId")
    void softDeleteAllByWorkspaceKeyAndMemberId(
            @Param("workspaceKey") String workspaceKey, @Param("memberId") Long memberId);

    @Query("""
                SELECT pm.memberId
                FROM ProjectMember pm
                WHERE pm.project = :project
                  AND pm.memberId IN :memberIds
                  AND pm.softDeleted = false
            """)
    Set<Long> findMemberIdsByProjectAndMemberIds(
            @Param("project") Project project, @Param("memberIds") Collection<Long> memberIds);

    @Query("""
            SELECT
                pm.memberId as memberId,
                wm.member.email as email,
                wm.member.language as language
            FROM ProjectMember pm
            JOIN pm.workspaceMember wm
            WHERE pm.workspaceKey = :workspaceKey
            AND pm.projectKey = :projectKey
            AND pm.softDeleted = false
            """)
    List<WorkspaceMemberContactInfo> findAllContactsByProjectKey(
            @Param("workspaceKey") String workspaceKey, @Param("projectKey") String projectKey);

    @Query("""
            SELECT
                pm.memberId as memberId,
                wm.member.email as email,
                wm.member.language as language
            FROM ProjectMember pm
            JOIN pm.workspaceMember wm
            WHERE pm.workspaceKey = :workspaceKey
            AND pm.projectKey = :projectKey
            AND pm.memberId <> :excludedMemberId
            AND pm.softDeleted = false
            """)
    List<WorkspaceMemberContactInfo> findAllContactsByProjectKeyExcluding(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("excludedMemberId") Long excludedMemberId);
}
