package com.tissue.issue.adapter.out.persistence.repository;

import com.tissue.issue.application.dto.IssueCountProjection;
import com.tissue.issue.application.dto.IssueCountStats;
import com.tissue.issue.application.dto.IssuePointStats;
import com.tissue.issue.domain.Issue;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.domain.Project;
import com.tissue.sprint.domain.Sprint;
import com.tissue.workflow.domain.enums.StateCategory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueJpaRepository extends Repository<Issue, Long> {

    Optional<Issue> findById(Long id);

    @EntityGraph(attributePaths = {"project", "issueType", "issueType.workflow", "currentState"})
    @Query("SELECT i FROM Issue i WHERE i.workspaceKey = :workspaceKey AND i.key.value = :issueKey")
    Optional<Issue> findWithBasicInfo(@Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @EntityGraph(
            attributePaths = {
                "project",
                "issueType",
                "currentState",
                "participants.assignee",
                "participants.assignee.workspaceMember",
                "participants.reporter",
                "participants.reporter.workspaceMember"
            })
    @Query("SELECT i FROM Issue i WHERE i.workspaceKey = :workspaceKey AND i.key.value = :issueKey")
    Optional<Issue> findWithDetail(@Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query("SELECT i FROM Issue i WHERE i.key.value = :issueKey AND i.workspaceKey = :workspaceKey")
    Optional<Issue> findByKeyAndWorkspaceKey(
            @Param("issueKey") String issueKey, @Param("workspaceKey") String workspaceKey);

    @Query("SELECT i FROM Issue i WHERE i.key.value = :issueKey AND i.project = :project")
    Optional<Issue> findByKeyAndProject(@Param("issueKey") String issueKey, @Param("project") Project project);

    @Query("SELECT i FROM Issue i WHERE i.key.value IN :issueKeys AND i.workspaceKey = :workspaceKey")
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

    boolean existsByIssueType(IssueType issueType);

    @Query("""
                SELECT COALESCE(SUM(i.storyPoint), 0)
                FROM Issue i
                WHERE i.parentIssue.id = :parentId
                  AND i.softDeleted = false
            """)
    Integer sumChildrenStoryPoints(@Param("parentId") Long parentId);

    @Query("""
                SELECT new com.tissue.issue.application.dto.IssueCountStats(
                    COUNT(i),
                    SUM(CASE WHEN i.currentState.category = com.tissue.workflow.domain.enums.StateCategory.COMPLETED
                    THEN 1 ELSE 0 END)
                )
                FROM Issue i
                WHERE i.parentIssue.id = :parentId
                AND i.softDeleted = false
            """)
    IssueCountStats getChildIssueStats(@Param("parentId") Long parentId);

    @Query("""
                SELECT new com.tissue.issue.application.dto.IssuePointStats(
                    COALESCE(SUM(i.storyPoint), 0),
                    COALESCE(SUM(CASE WHEN i.currentState.category = com.tissue.workflow.domain.enums.StateCategory.COMPLETED
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
                SELECT new com.tissue.issue.application.dto.IssueCountProjection(
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
}
