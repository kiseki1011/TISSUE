package com.tissue.feature.project.application.port.repository;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberContactInfo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberQueryRepository extends Repository<ProjectMember, Long> {

    @Query("""
            SELECT pm
            FROM ProjectMember pm
            JOIN FETCH pm.workspaceMember wm
            WHERE wm.member.email = :email
              AND pm.projectKey = :projectKey
              AND pm.workspaceKey = :workspaceKey
              AND pm.softDeleted = false
            """)
    Optional<ProjectMember> findWithWorkspaceMemberByEmailAndKeys(
            @Param("email") String email,
            @Param("projectKey") String projectKey,
            @Param("workspaceKey") String workspaceKey);

    @Query("""
            SELECT pm
            FROM ProjectMember pm
            JOIN FETCH pm.workspaceMember wm
            WHERE pm.workspaceKey = :workspaceKey
              AND pm.projectKey = :projectKey
              AND pm.memberId = :memberId
              AND pm.softDeleted = false
            """)
    Optional<ProjectMember> findWithWorkspaceMemberByKeysAndMemberId(
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
              AND pm.softDeleted = false
            """)
    Optional<ProjectMember> findWithProjectByKeys(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("memberId") Long memberId);

    @Query("""
            SELECT pm
            FROM ProjectMember pm
            WHERE pm.project = :project
              AND pm.memberId = :memberId
              AND pm.softDeleted = false
            """)
    Optional<ProjectMember> findByProjectAndMemberId(
            @Param("project") Project project, @Param("memberId") Long memberId);

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

    @Query("""
            SELECT pm
            FROM ProjectMember pm
            WHERE pm.project = :project
              AND pm.memberId = :memberId
            """)
    Optional<ProjectMember> findByProjectAndMemberIdIncludingSoftDeleted(
            @Param("project") Project project, @Param("memberId") Long memberId);

    @Query("""
            SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END
            FROM ProjectMember pm
            WHERE pm.project = :project
              AND pm.memberId = :memberId
            """)
    boolean existsByProjectAndMemberIdIncludingSoftDeleted(
            @Param("project") Project project, @Param("memberId") Long memberId);
}
