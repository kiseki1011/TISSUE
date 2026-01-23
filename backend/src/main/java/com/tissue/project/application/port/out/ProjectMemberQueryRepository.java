package com.tissue.project.application.port.out;

import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.workspace.application.port.out.WorkspaceMemberContact;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberQueryRepository extends Repository<ProjectMember, Long> {

    String WORKSPACE_MEMBER_CONTACT_PATH = "com.tissue.workspace.application.port.out.";

    @Query("SELECT pm " + "FROM ProjectMember pm "
            + "JOIN FETCH pm.workspaceMember wm "
            + "WHERE wm.member.email = :email "
            + "AND pm.projectKey = :projectKey "
            + "AND pm.softDeleted = false")
    Optional<ProjectMember> findByEmailAndProjectKey(
            @Param("email") String email, @Param("projectKey") String projectKey);

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

    // TODO: WorkspaceMember와 같이 조회(JOIN FETCH)
    Optional<ProjectMember> findByProjectIdAndMemberId(Long projectId, Long memberId);

    // TODO: WorkspaceMember와 같이 조회(JOIN FETCH)
    Optional<ProjectMember> findByProjectIdAndMemberIdAndSoftDeletedFalse(Long projectId, Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProjectMember pm SET pm.softDeleted = true, pm.softDeletedAt = CURRENT_TIMESTAMP, "
            + "pm.archived = true, pm.archivedAt = CURRENT_TIMESTAMP "
            + "WHERE pm.workspaceKey = :workspaceKey AND pm.memberId = :memberId")
    void softDeleteAllByWorkspaceKeyAndMemberId(
            @Param("workspaceKey") String workspaceKey, @Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProjectMember pm SET pm.softDeleted = true, pm.softDeletedAt = CURRENT_TIMESTAMP, "
            + "pm.archived = true, pm.archivedAt = CURRENT_TIMESTAMP "
            + "WHERE pm.memberId = :memberId")
    void softDeleteAllByMemberId(@Param("memberId") Long memberId);

    @Query("""
                SELECT pm.memberId
                FROM ProjectMember pm
                WHERE pm.project = :project
                  AND pm.memberId IN :memberIds
                  AND pm.softDeleted = false
            """)
    Set<Long> findMemberIdsByProjectAndMemberIds(
            @Param("project") Project project, @Param("memberIds") Collection<Long> memberIds);

    boolean existsByProjectAndMemberId(Project project, Long memberId);

    @Query("SELECT new " + WORKSPACE_MEMBER_CONTACT_PATH
            + "WorkspaceMemberContact(pm.memberId, wm.member.email, wm.member.language) "
            + "FROM ProjectMember pm "
            + "JOIN pm.workspaceMember wm "
            + "WHERE pm.workspaceKey = :workspaceKey "
            + "AND pm.projectKey = :projectKey "
            + "AND pm.softDeleted = false")
    List<WorkspaceMemberContact> findAllContactsByProjectKey(
            @Param("workspaceKey") String workspaceKey, @Param("projectKey") String projectKey);

    @Query("SELECT new " + WORKSPACE_MEMBER_CONTACT_PATH
            + "WorkspaceMemberContact(pm.memberId, wm.member.email, wm.member.language) "
            + "FROM ProjectMember pm "
            + "JOIN pm.workspaceMember wm "
            + "WHERE pm.workspaceKey = :workspaceKey "
            + "AND pm.projectKey = :projectKey "
            + "AND pm.memberId <> :excludedMemberId "
            + "AND pm.softDeleted = false")
    List<WorkspaceMemberContact> findAllContactsByProjectKeyExcluding(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("excludedMemberId") Long excludedMemberId);

    @Query("SELECT new " + WORKSPACE_MEMBER_CONTACT_PATH
            + "WorkspaceMemberContact(pm.memberId, wm.member.email, wm.member.language) "
            + "FROM ProjectMember pm "
            + "JOIN pm.workspaceMember wm "
            + "WHERE pm.workspaceKey = :workspaceKey "
            + "AND pm.projectKey = :projectKey "
            + "AND pm.role = :role "
            + "AND pm.softDeleted = false")
    List<WorkspaceMemberContact> findAdminContactsByProjectKey(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("role") ProjectRole role);
}
