package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.application.dto.IssueCountProjection;
import com.tissue.feature.issue.application.dto.IssueCountStats;
import com.tissue.feature.issue.application.dto.IssuePointStats;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueQueryRepository extends Repository<Issue, Long> {

    Optional<Issue> findById(Long id);

    @Query("SELECT i FROM Issue i WHERE i.key.value = :issueKey")
    Optional<Issue> findByKey(@Param("issueKey") String issueKey);

    @Query("""
           SELECT i
           FROM Issue i
           JOIN FETCH i.project p
           WHERE i.key.value = :issueKey
       """)
    Optional<Issue> findWithProjectByKey(@Param("issueKey") String issueKey);

    @EntityGraph(attributePaths = {"project", "issueType"})
    @Query("SELECT i FROM Issue i WHERE i.key.value = :issueKey")
    Optional<Issue> findWithProjectAndIssueTypeByKey(@Param("issueKey") String issueKey);

    @Query("SELECT i FROM Issue i WHERE i.key.value IN :issueKeys")
    List<Issue> findByKeyIn(@Param("issueKeys") Collection<String> issueKeys);

    @Query(value = """
           SELECT i.*
           FROM issue i
           JOIN project p ON i.project_id = p.id
           WHERE i.issue_key = :issueKey
             AND i.soft_deleted = true
       """, nativeQuery = true)
    Optional<Issue> findDeletedWithProjectByKey(@Param("issueKey") String issueKey);

    @EntityGraph(attributePaths = {"project", "issueType", "issueType.workflow", "currentState"})
    @Query("SELECT i FROM Issue i WHERE i.key.value = :issueKey")
    Optional<Issue> findWithBasicInfoByKey(@Param("issueKey") String issueKey);

    @EntityGraph(attributePaths = {"project", "parentIssue", "parentIssue.issueType"})
    @Query("SELECT i FROM Issue i WHERE i.key.value = :issueKey")
    Optional<Issue> findWithParentByKey(@Param("issueKey") String issueKey);

    @EntityGraph(attributePaths = {"issueType", "parentIssue", "parentIssue.project"})
    @Query("""
            SELECT child
            FROM Issue child
            WHERE child.parentIssue.key.value = :issueKey
            ORDER BY child.createdAt ASC
        """)
    List<Issue> findChildrenByParentKey(@Param("issueKey") String issueKey);

    @Query("""
            SELECT COUNT(child) > 0
            FROM Issue child
            WHERE child.parentIssue.key.value = :issueKey
        """)
    boolean hasChildren(@Param("issueKey") String issueKey);

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
            SELECT COUNT(i) AS totalCount,
                SUM(CASE WHEN i.currentState.category = :completed THEN 1 ELSE 0 END) AS doneCount
            FROM Issue i
            WHERE i.parentIssue.id = :parentId
              AND i.softDeleted = false
              AND i.currentState.category != :aborted
        """)
    IssueCountStats getChildIssueStats(
            @Param("parentId") Long parentId,
            @Param("completed") StateCategory completed,
            @Param("aborted") StateCategory aborted);

    @Query("""
            SELECT COALESCE(SUM(i.storyPoint), 0) AS totalPoints,
                COALESCE(SUM(CASE WHEN i.currentState.category = :completed
                THEN i.storyPoint ELSE 0 END), 0) AS donePoints
            FROM Issue i
            WHERE i.parentIssue.id = :parentId
              AND i.softDeleted = false
              AND i.currentState.category != :aborted
        """)
    IssuePointStats getChildPointStats(
            @Param("parentId") Long parentId,
            @Param("completed") StateCategory completed,
            @Param("aborted") StateCategory aborted);

    @Query("""
            SELECT i FROM Issue i
            WHERE i.sprint = :sprint
        """)
    List<Issue> findAllBySprint(@Param("sprint") Sprint sprint);

    @Query("""
            SELECT i FROM Issue i
            WHERE i.sprint = :sprint
              AND i.currentState.category NOT IN :terminalCategories
        """)
    List<Issue> findIncompleteIssuesBySprint(
            @Param("sprint") Sprint sprint, @Param("terminalCategories") Collection<StateCategory> terminalCategories);

    @Query("""
            SELECT i.key.value
            FROM Issue i
            WHERE i.sprint = :sprint
              AND i.currentState.category NOT IN :terminalCategories
        """)
    List<String> findIncompleteIssueKeysBySprint(
            @Param("sprint") Sprint sprint, @Param("terminalCategories") Collection<StateCategory> terminalCategories);

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
            SELECT i.currentState.id AS stateId,
                COUNT(i) AS count
            FROM Issue i
            WHERE i.currentState.id IN :stateIds
              AND i.softDeleted = false
            GROUP BY i.currentState.id
        """)
    List<IssueCountProjection> findActiveIssueCounts(@Param("stateIds") Collection<Long> stateIds);

    @Query("""
            SELECT i.createdBy
            FROM Issue i
            WHERE i.key.value = :issueKey
            """)
    Optional<Long> findAuthorId(@Param("issueKey") String issueKey);

    @Query("""
            SELECT pm.member.id
            FROM Issue i
            JOIN i.participants.assignee pm
            WHERE i.key.value = :issueKey
            """)
    Optional<Long> findAssigneeMemberId(@Param("issueKey") String issueKey);

    @Query("""
            SELECT pm.member.id
            FROM IssueReviewer ir
            JOIN ir.issue i
            JOIN ir.reviewer pm
            WHERE i.key.value = :issueKey
            """)
    Set<Long> findReviewerMemberIds(@Param("issueKey") String issueKey);

    @Query("""
            SELECT pm.member.id
            FROM IssueSubscriber isub
            JOIN isub.issue i
            JOIN isub.subscriber pm
            WHERE i.key.value = :issueKey
            """)
    Set<Long> findSubscriberMemberIds(@Param("issueKey") String issueKey);

    @Query("""
            SELECT child.key.value
            FROM Issue child
            WHERE child.parentIssue.id = :parentId
              AND child.softDeleted = false
              AND child.currentState.category NOT IN :terminalCategories
        """)
    List<String> findUnresolvedChildKeys(
            @Param("parentId") Long parentId,
            @Param("terminalCategories") Collection<StateCategory> terminalCategories);
}
