package com.tissue.issue.application.port.out;

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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// TODO: consider making a abstracted interface and move this to
// adapter.out.persistence.IssueQueryJpaAdpater
//  reason: i think i might change the implementation of query methods a lot to test performance
public interface IssueQueryRepository extends Repository<Issue, Long> {

    Optional<Issue> findById(Long id);

    @Query("SELECT i FROM Issue i WHERE i.key.value = :issueKey AND i.workspaceKey = :workspaceKey")
    Optional<Issue> findByKeyAndWorkspaceKey(
            @Param("issueKey") String issueKey, @Param("workspaceKey") String workspaceKey);

    @Query("SELECT i FROM Issue i WHERE i.key.value = :issueKey AND i.project = :project")
    Optional<Issue> findByKeyAndProject(
            @Param("issueKey") String issueKey, @Param("project") Project project);

    @Query(
            "SELECT i FROM Issue i WHERE i.key.value IN :issueKeys AND i.workspaceKey ="
                    + " :workspaceKey")
    List<Issue> findByKeyInAndWorkspaceKey(
            @Param("issueKeys") Collection<String> issueKeys,
            @Param("workspaceKey") String workspaceKey);

    @Query(
            """
                SELECT i
                FROM Issue i
                JOIN FETCH i.project p
                JOIN FETCH i.issueType it
                JOIN FETCH it.workflow
                JOIN FETCH i.currentState
                WHERE p.workspaceKey = :workspaceKey AND i.key.value = :issueKey
            """)
    Optional<Issue> findWithBasicInfo(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query(
            """
                SELECT i
                FROM Issue i
                JOIN FETCH i.project p
                JOIN FETCH i.issueType it
                JOIN FETCH i.currentState cs
                LEFT JOIN FETCH i.participants.assignee a
                LEFT JOIN FETCH a.workspaceMember awm
                JOIN FETCH i.participants.reporter r
                JOIN FETCH r.workspaceMember rwm
                WHERE p.workspaceKey = :workspaceKey AND i.key.value = :issueKey
            """)
    Optional<Issue> findWithDetail(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query(
            """
                SELECT i
                FROM Issue i
                JOIN FETCH i.project p
                LEFT JOIN FETCH i.parentIssue pi
                LEFT JOIN FETCH pi.issueType pit
                WHERE p.workspaceKey = :workspaceKey AND i.key.value = :issueKey
            """)
    Optional<Issue> findWithParent(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query(
            """
                SELECT child
                FROM Issue child
                JOIN FETCH child.issueType it
                JOIN child.parentIssue pi
                JOIN pi.project p
                WHERE p.workspaceKey = :workspaceKey AND pi.key.value = :issueKey
                ORDER BY child.createdAt ASC
            """)
    List<Issue> findChildren(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query(
            """
                SELECT COUNT(child) > 0
                FROM Issue child
                JOIN child.parentIssue pi
                JOIN pi.project p
                WHERE p.workspaceKey = :workspaceKey AND pi.key.value = :issueKey
            """)
    boolean hasChildren(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    boolean existsByIssueType(IssueType issueType);

    @Query(
            """
                SELECT COALESCE(SUM(i.storyPoint), 0)
                FROM Issue i
                WHERE i.parentIssue.id = :parentId
                  AND i.softDeleted = false
            """)
    Integer sumChildrenStoryPoints(@Param("parentId") Long parentId);

    // spotless:off
    @Query(
            """
                SELECT com.tissue.issue.application.dto.IssueCountStats(
                    COUNT(i),
                    SUM(CASE WHEN i.currentState.category = com.tissue.workflow.domain.enums.StateCategory.COMPLETED
                    THEN 1 ELSE 0 END)
                )
                FROM Issue i
                WHERE i.parentIssue.id = :parentId
                AND i.softDeleted = false
            """)
    IssueCountStats getChildIssueStats(@Param("parentId") Long parentId);
    // spotless:on

    // spotless:off
    @Query(
            """
                SELECT new com.tissue.issue.application.dto.IssuePointStats(
                    COALESCE(SUM(i.storyPoint), 0),
                    COALESCE(SUM(CASE WHEN i.currentState.category = com.tissue.workflow.domain.enums.StateCategory.COMPLETED
                    THEN i.storyPoint ELSE 0 END), 0)
                )
                FROM Issue i
                WHERE i.parentIssue.id = :parentId AND i.softDeleted = false
            """)
    IssuePointStats getChildPointStats(@Param("parentId") Long parentId);
    // spotless:on

    @Query(
            """
                SELECT i FROM Issue i
                WHERE i.sprint = :sprint
                  AND i.currentState.category != :doneCategory
            """)
    List<Issue> findIncompleteIssuesBySprint(
            @Param("sprint") Sprint sprint, @Param("doneCategory") StateCategory doneCategory);

    @Query(
            """
                SELECT i.key.value
                FROM Issue i
                WHERE i.sprint = :sprint
                  AND i.currentState.category != :doneCategory
            """)
    List<String> findIncompleteIssueKeysBySprint(
            @Param("sprint") Sprint sprint, @Param("doneCategory") StateCategory doneCategory);

    @Query(
            """
                SELECT i.key.value
                FROM Issue i
                WHERE i.sprint = :sprint
            """)
    List<String> findIssueKeysBySprint(@Param("sprint") Sprint sprint);

    @Query(
            "SELECT DISTINCT i.currentState.id "
                    + "FROM Issue i "
                    + "WHERE i.currentState.id IN :stateIds "
                    + "AND i.softDeleted = false")
    List<Long> findStateIdsUsedByActiveIssues(@Param("stateIds") Collection<Long> stateIds);

    @Query(
            """
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

    /** Checks if the member is the author or assignee of the issue */
    @Query(
            """
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

    /** Checks if the member is the author of the issue */
    @Query(
            """
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
