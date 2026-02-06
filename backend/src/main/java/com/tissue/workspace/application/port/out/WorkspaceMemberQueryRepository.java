package com.tissue.workspace.application.port.out;

import com.tissue.member.domain.Member;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberQueryRepository extends Repository<WorkspaceMember, Long> {

    Optional<WorkspaceMember> findByWorkspaceKeyAndMember_Id(String workspaceKey, Long memberId);

    Optional<WorkspaceMember> findByWorkspaceAndMember_Id(Workspace workspace, Long memberId);

    Optional<WorkspaceMember> findByWorkspaceAndMember(Workspace workspace, Member member);

    List<WorkspaceMember> findAllByWorkspaceKeyAndMember_IdIn(String workspaceKey, Collection<Long> memberIds);

    @Query("""
       SELECT wm
       FROM WorkspaceMember wm
       JOIN FETCH wm.workspace
       WHERE wm.workspaceKey = :workspaceKey
         AND wm.member.id = :memberId
   """)
    Optional<WorkspaceMember> findWithWorkspaceByWorkspaceKeyAndMemberId(
            @Param("workspaceKey") String workspaceKey, @Param("memberId") Long memberId);

    @Query("""
       SELECT wm
       FROM WorkspaceMember wm
       JOIN FETCH wm.workspace
       JOIN FETCH wm.member
       WHERE wm.workspaceKey = :workspaceKey
         AND wm.member.id = :memberId
   """)
    Optional<WorkspaceMember> findWithWorkspaceAndMemberByWorkspaceKeyAndMemberId(
            @Param("workspaceKey") String workspaceKey, @Param("memberId") Long memberId);

    boolean existsByMemberAndRole(Member member, WorkspaceRole role);

    long countByWorkspaceKey(String workspaceKey);

    long countByMemberAndRole(Member member, WorkspaceRole role);

    long countByMember(Member member);

    @Query("SELECT wm.member.id FROM WorkspaceMember wm "
            + "WHERE wm.workspaceKey = :workspaceKey "
            + "AND wm.member.id IN :candidateIds "
            + "AND wm.softDeleted = false")
    Set<Long> findJoinedMemberIds(
            @Param("workspaceKey") String workspaceKey, @Param("candidateIds") Collection<Long> candidateIds);

    @Query("SELECT wm FROM WorkspaceMember wm "
            + "JOIN FETCH wm.workspace "
            + "WHERE wm.member.id = :memberId "
            + "AND wm.softDeleted = false "
            + "ORDER BY wm.createdAt DESC")
    List<WorkspaceMember> findAllWithWorkspaceByMemberId(@Param("memberId") Long memberId);

    @Query("""
            SELECT
                wm.member.id as memberId,
                wm.member.email as email,
                wm.member.language as language
            FROM WorkspaceMember wm
            WHERE wm.workspaceKey = :workspaceKey
            """)
    List<WorkspaceMemberContactInfo> findAllContactsByWorkspaceKey(@Param("workspaceKey") String workspaceKey);

    @Query("""
            SELECT
                wm.member.id as memberId,
                wm.member.email as email,
                wm.member.language as language
            FROM WorkspaceMember wm
            WHERE wm.workspaceKey = :workspaceKey
            AND wm.member.id <> :excludedMemberId
            """)
    List<WorkspaceMemberContactInfo> findAllContactsByWorkspaceKeyExcluding(
            @Param("workspaceKey") String workspaceKey, @Param("excludedMemberId") Long excludedMemberId);

    @Query("""
            SELECT
                wm.member.id as memberId,
                wm.member.email as email,
                wm.member.language as language
            FROM WorkspaceMember wm
            WHERE wm.workspaceKey = :workspaceKey
            AND wm.role IN :roles
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
            """)
    List<WorkspaceMemberContactInfo> findAllContactsByWorkspaceKeyAndUsernames(
            @Param("workspaceKey") String workspaceKey, @Param("usernames") Set<String> usernames);

    @Query("SELECT wm FROM WorkspaceMember wm "
            + "JOIN FETCH wm.member m "
            + "WHERE wm.workspaceKey = :workspaceKey "
            + "AND wm.softDeleted = false "
            + "AND (LOWER(wm.displayName) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "     OR LOWER(m.username) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<WorkspaceMember> searchMembers(@Param("workspaceKey") String workspaceKey, @Param("query") String query);

    @Query("SELECT wm FROM WorkspaceMember wm "
            + "JOIN FETCH wm.member m "
            + "WHERE wm.workspaceKey = :workspaceKey "
            + "AND wm.softDeleted = false "
            + "AND wm.member.id IN (SELECT pm.memberId FROM ProjectMember pm WHERE pm.projectKey = :projectKey) "
            + "AND (LOWER(wm.displayName) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "     OR LOWER(m.username) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<WorkspaceMember> searchProjectMembers(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("query") String query);
}
