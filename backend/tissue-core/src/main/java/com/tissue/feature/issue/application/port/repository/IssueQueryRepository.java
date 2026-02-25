package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.application.dto.IssueCountProjection;
import com.tissue.feature.issue.application.dto.IssueCountStats;
import com.tissue.feature.issue.application.dto.IssuePointStats;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberContactInfo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueQueryRepository extends Repository<Issue, Long> {

    @Query("""
           SELECT i
           FROM Issue i
           JOIN FETCH i.project p
           WHERE i.workspaceKey = :workspaceKey
             AND i.key.value = :issueKey
       """)
    Optional<Issue> findWithProjectByKeys(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    /**
     * Finds a soft-deleted issue. Since Issue entity has @SQLRestriction("soft_deleted = false"),
     * we need a native query to bypass it for restoration purposes.
     */
    @Query(value = """
           SELECT i.*
           FROM issue i
           JOIN project p ON i.project_id = p.id
           WHERE i.workspace_key = :workspaceKey
             AND i.issue_key = :issueKey
             AND i.soft_deleted = true
       """, nativeQuery = true)
    Optional<Issue> findDeletedWithProjectByKeys(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @EntityGraph(attributePaths = {"project", "issueType", "issueType.workflow", "currentState"})
    @Query("SELECT i FROM Issue i WHERE i.workspaceKey = :workspaceKey AND i.key.value = :issueKey")
    Optional<Issue> findWithBasicInfo(@Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @EntityGraph(
            attributePaths = {
                "project",
                "issueType",
                "currentState",
                "participants.assignee",
                "participants.assignee.workspaceMember"
            })
    @Query("SELECT i FROM Issue i WHERE i.workspaceKey = :workspaceKey AND i.key.value = :issueKey")
    Optional<Issue> findWithDetail(@Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @EntityGraph(attributePaths = {"project", "issueType", "fieldValues", "fieldValues.field"})
    @Query("SELECT i FROM Issue i WHERE i.workspaceKey = :workspaceKey AND i.key.value = :issueKey")
    Optional<Issue> findWithFieldValuesByKeys(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query("SELECT i FROM Issue i WHERE i.key.value = :issueKey AND i.workspaceKey = :workspaceKey")
    Optional<Issue> findByKeyAndWorkspaceKey(
            @Param("issueKey") String issueKey, @Param("workspaceKey") String workspaceKey);

    List<Issue> findByKeyInAndWorkspaceKey(
            @Param("issueKeys") Collection<String> issueKeys, @Param("workspaceKey") String workspaceKey);

    @EntityGraph(attributePaths = {"project", "parentIssue", "parentIssue.issueType"})
    @Query("SELECT i FROM Issue i WHERE i.workspaceKey = :workspaceKey AND i.key.value = :issueKey")
    Optional<Issue> findWithParent(@Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @EntityGraph(attributePaths = {"issueType", "parentIssue", "parentIssue.project"})
    @Query("""
            SELECT child
            FROM Issue child
            WHERE child.workspaceKey = :workspaceKey
            AND child.parentIssue.key.value = :issueKey
            ORDER BY child.createdAt ASC
        """)
    List<Issue> findChildren(@Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query("""
            SELECT COUNT(child) > 0
            FROM Issue child
            WHERE child.workspaceKey = :workspaceKey
            AND child.parentIssue.key.value = :issueKey
        """)
    boolean hasChildren(@Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query("SELECT COUNT(i) > 0 FROM Issue i WHERE i.issueType = :issueType")
    boolean existsByIssueType(@Param("issueType") IssueType issueType);

    @Query("""
            SELECT COALESCE(SUM(i.storyPoint), 0)
            FROM Issue i
            WHERE i.parentIssue.id = :parentId
              AND i.softDeleted = false
        """)
    Integer sumChildrenStoryPoints(@Param("parentId") Long parentId);

    @Query("""
            SELECT new com.tissue.feature.issue.application.dto.IssueCountStats(
                COUNT(i),
                SUM(CASE WHEN i.currentState.category = com.tissue.feature.workflow.domain.enums.StateCategory.COMPLETED
                THEN 1 ELSE 0 END)
            )
            FROM Issue i
            WHERE i.parentIssue.id = :parentId
            AND i.softDeleted = false
        """)
    IssueCountStats getChildIssueStats(@Param("parentId") Long parentId);

    @Query("""
            SELECT new com.tissue.feature.issue.application.dto.IssuePointStats(
                COALESCE(SUM(i.storyPoint), 0),
                COALESCE(SUM(CASE WHEN i.currentState.category = com.tissue.feature.workflow.domain.enums.StateCategory.COMPLETED
                THEN i.storyPoint ELSE 0 END), 0)
            )
            FROM Issue i
            WHERE i.parentIssue.id = :parentId AND i.softDeleted = false
        """)
    IssuePointStats getChildPointStats(@Param("parentId") Long parentId);

    @Query("""
            SELECT i FROM Issue i
            WHERE i.sprint = :sprint
              AND i.currentState.category != :doneCategory
        """)
    List<Issue> findIncompleteIssuesBySprint(
            @Param("sprint") Sprint sprint, @Param("doneCategory") StateCategory doneCategory);

    @Query("""
            SELECT i.key.value
            FROM Issue i
            WHERE i.sprint = :sprint
              AND i.currentState.category != :doneCategory
        """)
    List<String> findIncompleteIssueKeysBySprint(
            @Param("sprint") Sprint sprint, @Param("doneCategory") StateCategory doneCategory);

    @Query("""
            SELECT i.key.value
            FROM Issue i
            WHERE i.sprint = :sprint
        """)
    List<String> findIssueKeysBySprint(@Param("sprint") Sprint sprint);

    @Query("SELECT DISTINCT i.currentState.id "
            + "FROM Issue i "
            + "WHERE i.currentState.id IN :stateIds "
            + "AND i.softDeleted = false")
    List<Long> findStateIdsUsedByActiveIssues(@Param("stateIds") Collection<Long> stateIds);

    @Query("""
            SELECT new com.tissue.feature.issue.application.dto.IssueCountProjection(
                i.currentState.id,
                COUNT(i)
            )
            FROM Issue i
            WHERE i.currentState.id IN :stateIds
              AND i.softDeleted = false
            GROUP BY i.currentState.id
        """)
    List<IssueCountProjection> findActiveIssueCounts(@Param("stateIds") Collection<Long> stateIds);

    @Query("""
            SELECT COUNT(i) > 0
            FROM Issue i
            WHERE i.workspaceKey = :workspaceKey
              AND i.key.value = :issueKey
              AND (
                  i.createdBy = :memberId
                  OR i.participants.assignee.memberId = :memberId
              )
        """)
    boolean isAuthorOrAssignee(
            @Param("workspaceKey") String workspaceKey,
            @Param("issueKey") String issueKey,
            @Param("memberId") Long memberId);

    @Query("""
            SELECT COUNT(i) > 0
            FROM Issue i
            WHERE i.workspaceKey = :workspaceKey
              AND i.key.value = :issueKey
              AND i.createdBy = :memberId
        """)
    boolean isAuthor(
            @Param("workspaceKey") String workspaceKey,
            @Param("issueKey") String issueKey,
            @Param("memberId") Long memberId);

    @Query("""
            SELECT
                m.id as memberId,
                m.email as email,
                m.language as language
            FROM Issue i
            JOIN i.project p
            JOIN WorkspaceMember wm ON wm.member.id = i.createdBy AND wm.workspace.id = p.workspace.id
            JOIN wm.member m
            WHERE i.workspaceKey = :workspaceKey
            AND i.key.value = :issueKey
            """)
    Optional<WorkspaceMemberContactInfo> findAuthorContact(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query("""
            SELECT
                m.id as memberId,
                m.email as email,
                m.language as language
            FROM Issue i
            JOIN i.participants.assignee pm
            JOIN pm.workspaceMember wm
            JOIN wm.member m
            WHERE i.workspaceKey = :workspaceKey
            AND i.key.value = :issueKey
            """)
    Optional<WorkspaceMemberContactInfo> findAssigneeContact(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query("""
            SELECT
                m.id as memberId,
                m.email as email,
                m.language as language
            FROM IssueReviewer r
            JOIN r.issue i
            JOIN r.reviewer pm
            JOIN pm.workspaceMember wm
            JOIN wm.member m
            WHERE i.workspaceKey = :workspaceKey
            AND i.key.value = :issueKey
            """)
    List<WorkspaceMemberContactInfo> findReviewerContacts(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query("""
            SELECT
                m.id as memberId,
                m.email as email,
                m.language as language
            FROM IssueSubscriber s
            JOIN s.issue i
            JOIN s.subscriber pm
            JOIN pm.workspaceMember wm
            JOIN wm.member m
            WHERE i.workspaceKey = :workspaceKey
            AND i.key.value = :issueKey
            """)
    List<WorkspaceMemberContactInfo> findSubscriberContacts(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);
}
