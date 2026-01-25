package com.tissue.workspace.application.port.out;

import com.tissue.member.domain.Member;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberQueryRepository extends Repository<WorkspaceMember, Long> {

    String WORKSPACE_MEMBER_CONTACT_PATH = "com.tissue.workspace.application.port.out.";

    // TODO: 굳이 Member_Id로 조회할 필요가 있나? 어차피 memberId 필드가 WorkspaceMember 내부에 있을텐데?
    Optional<WorkspaceMember> findByMember_IdAndWorkspaceKey(Long memberId, String workspaceKey);

    Optional<WorkspaceMember> findByMember_EmailAndWorkspaceKey(String email, String workspaceKey);

    Optional<WorkspaceMember> findByMember_IdAndWorkspace(Long memberId, Workspace workspace);

    Optional<WorkspaceMember> findByMemberAndWorkspace(Member member, Workspace workspace);

    Optional<WorkspaceMember> findByMember_IdAndWorkspaceKeyAndSoftDeletedFalse(Long memberId, String workspaceKey);

    Optional<WorkspaceMember> findByMember_IdAndWorkspaceAndSoftDeletedFalse(Long memberId, Workspace workspace);

    Optional<WorkspaceMember> findByMemberAndWorkspaceAndSoftDeletedFalse(Member member, Workspace workspace);

    Optional<WorkspaceMember> findByIdAndSoftDeletedFalse(Long workspaceMemberId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE WorkspaceMember wm SET wm.softDeleted = true, wm.softDeletedAt = CURRENT_TIMESTAMP, "
            + "wm.archived = true, wm.archivedAt = CURRENT_TIMESTAMP "
            + "WHERE wm.member.id = :memberId")
    void softDeleteAllByMemberId(@Param("memberId") Long memberId);

    List<WorkspaceMember> findAllByWorkspace_Key(String workspaceKey);

    @Query("SELECT wm FROM WorkspaceMember wm " + "WHERE wm.workspace.key = :workspaceKey AND wm.role IN :roles")
    Set<WorkspaceMember> findAdminsByWorkspace_Key(
            @Param("workspaceKey") String workspaceKey, @Param("roles") Set<WorkspaceRole> roles);

    List<WorkspaceMember> findAllByMember_IdInAndWorkspaceKey(Collection<Long> memberIds, String workspaceKey);

    @Query("SELECT wm.member.id FROM WorkspaceMember wm "
            + "WHERE wm.workspaceKey = :workspaceKey "
            + "AND wm.member.id IN :candidateIds "
            + "AND wm.softDeleted = false")
    Set<Long> findJoinedMemberIds(
            @Param("workspaceKey") String workspaceKey, @Param("candidateIds") Collection<Long> candidateIds);

    boolean existsByMemberAndRole(Member member, WorkspaceRole role);

    boolean existsByMemberAndWorkspace(Member member, Workspace workspace);

    long countByWorkspaceKey(String workspaceKey);

    long countByMemberAndRole(Member member, WorkspaceRole role);

    long countByMember(Member member);

    List<WorkspaceMember> findAllByMember(Member member);

    @Query("SELECT wm FROM WorkspaceMember wm "
            + "JOIN FETCH wm.workspace "
            + "WHERE wm.member.id = :memberId "
            + "AND wm.softDeleted = false "
            + "ORDER BY wm.createdAt DESC")
    List<WorkspaceMember> findAllByMemberIdWithWorkspace(@Param("memberId") Long memberId);

    @Query("SELECT new " + WORKSPACE_MEMBER_CONTACT_PATH
            + "WorkspaceMemberContact(wm.member.id, wm.member.email, wm.member.language) "
            + "FROM WorkspaceMember wm WHERE wm.workspaceKey = :workspaceKey")
    List<WorkspaceMemberContact> findAllContactsByWorkspaceKey(@Param("workspaceKey") String workspaceKey);

    @Query("SELECT new " + WORKSPACE_MEMBER_CONTACT_PATH
            + "WorkspaceMemberContact(wm.member.id, wm.member.email, wm.member.language) "
            + "FROM WorkspaceMember wm WHERE wm.workspaceKey = :workspaceKey AND wm.member.id <> :excludedMemberId")
    List<WorkspaceMemberContact> findAllContactsByWorkspaceKeyExcluding(
            @Param("workspaceKey") String workspaceKey, @Param("excludedMemberId") Long excludedMemberId);

    @Query("SELECT new " + WORKSPACE_MEMBER_CONTACT_PATH
            + "WorkspaceMemberContact(wm.member.id, wm.member.email, wm.member.language) "
            + "FROM WorkspaceMember wm WHERE wm.workspaceKey = :workspaceKey AND wm.role IN :roles")
    Set<WorkspaceMemberContact> findAdminContactsByWorkspace_Key(
            @Param("workspaceKey") String workspaceKey, @Param("roles") Set<WorkspaceRole> roles);

    @Query("SELECT new " + WORKSPACE_MEMBER_CONTACT_PATH
            + "WorkspaceMemberContact(wm.member.id, wm.member.email, wm.member.language) "
            + "FROM WorkspaceMember wm WHERE wm.member.id = :memberId AND wm.workspaceKey = :workspaceKey")
    Optional<WorkspaceMemberContact> findContactByMemberIdAndWorkspaceKey(
            @Param("memberId") Long memberId, @Param("workspaceKey") String workspaceKey);

    @Query("SELECT new " + WORKSPACE_MEMBER_CONTACT_PATH
            + "WorkspaceMemberContact(wm.member.id, wm.member.email, wm.member.language) "
            + "FROM WorkspaceMember wm WHERE wm.workspaceKey = :workspaceKey AND wm.member.id IN :memberIds")
    List<WorkspaceMemberContact> findAllContactsByWorkspaceKeyAndMemberIds(
            @Param("workspaceKey") String workspaceKey, @Param("memberIds") Collection<Long> memberIds);

    @Query("SELECT new " + WORKSPACE_MEMBER_CONTACT_PATH
            + "WorkspaceMemberContact(wm.member.id, wm.member.email, wm.member.language) "
            + "FROM WorkspaceMember wm WHERE wm.workspaceKey = :workspaceKey AND wm.member.username IN :usernames")
    List<WorkspaceMemberContact> findAllContactsByWorkspaceKeyAndUsernames(
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
