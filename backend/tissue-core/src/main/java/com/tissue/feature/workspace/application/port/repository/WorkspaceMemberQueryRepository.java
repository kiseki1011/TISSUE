package com.tissue.feature.workspace.application.port.repository;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberQueryRepository extends Repository<WorkspaceMember, Long> {

    @Query("""
           SELECT wm
           FROM WorkspaceMember wm
           JOIN FETCH wm.workspace w
           JOIN FETCH wm.member m
           WHERE w.key = :workspaceKey
             AND m.id = :memberId
             AND wm.softDeleted = false
       """)
    Optional<WorkspaceMember> findWithWorkspaceByWorkspaceKeyAndMemberId(
            @Param("workspaceKey") String workspaceKey, @Param("memberId") Long memberId);

    @Query("""
           SELECT wm
           FROM WorkspaceMember wm
           WHERE wm.workspace = :workspace
             AND wm.member = :member
       """)
    Optional<WorkspaceMember> findByWorkspaceAndMemberIncludingSoftDeleted(
            @Param("workspace") Workspace workspace, @Param("member") Member member);

    @Query("""
           SELECT wm
           FROM WorkspaceMember wm
           WHERE wm.workspaceKey = :workspaceKey
             AND wm.member.id IN :memberIds
       """)
    List<WorkspaceMember> findAllByWorkspaceKeyAndMemberIdsIncludingSoftDeleted(
            @Param("workspaceKey") String workspaceKey, @Param("memberIds") Collection<Long> memberIds);

    @Query("""
           SELECT CASE WHEN COUNT(wm) > 0 THEN true ELSE false END
           FROM WorkspaceMember wm
           WHERE wm.member = :member
             AND wm.role = :role
             AND wm.softDeleted = false
       """)
    boolean existsByMemberAndRole(@Param("member") Member member, @Param("role") WorkspaceRole role);

    @Query("""
           SELECT COUNT(wm)
           FROM WorkspaceMember wm
           WHERE wm.workspaceKey = :workspaceKey
       """)
    long countByWorkspaceKeyIncludingSoftDeleted(@Param("workspaceKey") String workspaceKey);

    @Query("""
           SELECT COUNT(wm)
           FROM WorkspaceMember wm
           WHERE wm.member = :member
             AND wm.role = :role
             AND wm.softDeleted = false
       """)
    long countByMemberAndRole(@Param("member") Member member, @Param("role") WorkspaceRole role);

    @Query("""
           SELECT COUNT(wm)
           FROM WorkspaceMember wm
           WHERE wm.member = :member
             AND wm.softDeleted = false
       """)
    long countByMember(@Param("member") Member member);

    @Query("""
           SELECT wm.member.id
           FROM WorkspaceMember wm
           WHERE wm.workspaceKey = :workspaceKey
             AND wm.member.id IN :candidateIds
             AND wm.softDeleted = false
       """)
    Set<Long> findJoinedMemberIds(
            @Param("workspaceKey") String workspaceKey, @Param("candidateIds") Collection<Long> candidateIds);

    @Query("""
           SELECT wm
           FROM WorkspaceMember wm
           JOIN FETCH wm.workspace
           WHERE wm.member.id = :memberId
             AND wm.softDeleted = false
           ORDER BY wm.createdAt DESC
       """)
    List<WorkspaceMember> findAllWithWorkspaceByMemberId(@Param("memberId") Long memberId);

    @Query("""
            SELECT
                wm.member.id as memberId,
                wm.member.email as email,
                wm.member.language as language
            FROM WorkspaceMember wm
            WHERE wm.workspaceKey = :workspaceKey
            AND wm.role IN :roles
            AND wm.softDeleted = false
            """)
    Set<WorkspaceMemberContactInfo> findAdminContactsByWorkspace_Key(
            @Param("workspaceKey") String workspaceKey, @Param("roles") Set<WorkspaceRole> roles);

    @Query("""
            SELECT
                wm.member.id as memberId,
                wm.member.email as email,
                wm.member.language as language
            FROM WorkspaceMember wm
            WHERE wm.member.id = :memberId
            AND wm.workspaceKey = :workspaceKey
            AND wm.softDeleted = false
            """)
    Optional<WorkspaceMemberContactInfo> findContactByMemberIdAndWorkspaceKey(
            @Param("memberId") Long memberId, @Param("workspaceKey") String workspaceKey);

    @Query("""
            SELECT
                wm.member.id as memberId,
                wm.member.email as email,
                wm.member.language as language
            FROM WorkspaceMember wm
            WHERE wm.workspaceKey = :workspaceKey
            AND wm.member.id IN :memberIds
            AND wm.softDeleted = false
            """)
    List<WorkspaceMemberContactInfo> findAllContactsByWorkspaceKeyAndMemberIds(
            @Param("workspaceKey") String workspaceKey, @Param("memberIds") Collection<Long> memberIds);

    @Query("""
            SELECT
                wm.member.id as memberId,
                wm.member.email as email,
                wm.member.language as language
            FROM WorkspaceMember wm
            WHERE wm.workspaceKey = :workspaceKey
            AND wm.member.username IN :usernames
            AND wm.softDeleted = false
            """)
    List<WorkspaceMemberContactInfo> findAllContactsByWorkspaceKeyAndUsernames(
            @Param("workspaceKey") String workspaceKey, @Param("usernames") Set<String> usernames);

    @Query("""
           SELECT wm
           FROM WorkspaceMember wm
           JOIN FETCH wm.member m
           WHERE wm.workspaceKey = :workspaceKey
             AND wm.softDeleted = false
             AND (LOWER(wm.displayName) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(m.username) LIKE LOWER(CONCAT('%', :query, '%')))
       """)
    List<WorkspaceMember> searchMembers(@Param("workspaceKey") String workspaceKey, @Param("query") String query);

    @Query("""
           SELECT wm
           FROM WorkspaceMember wm
           JOIN FETCH wm.member m
           WHERE wm.workspaceKey = :workspaceKey
             AND wm.softDeleted = false
             AND wm.member.id IN (SELECT pm.memberId FROM ProjectMember pm WHERE pm.projectKey = :projectKey)
             AND (LOWER(wm.displayName) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(m.username) LIKE LOWER(CONCAT('%', :query, '%')))
       """)
    List<WorkspaceMember> searchProjectMembers(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("query") String query);

    @Query("""
           SELECT wm
           FROM WorkspaceMember wm
           WHERE wm.workspaceKey = :workspaceKey
             AND wm.member.id = :memberId
             AND wm.softDeleted = false
       """)
    Optional<WorkspaceMember> findByWorkspaceKeyAndMemberId(
            @Param("workspaceKey") String workspaceKey, @Param("memberId") Long memberId);
}
