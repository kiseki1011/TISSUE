package com.tissue.api.issue.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.application.dto.IssueCountProjection;
import com.tissue.api.issue.application.dto.IssueCountStats;
import com.tissue.api.issue.application.dto.IssuePointStats;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.project.domain.Project;
import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.workflow.domain.enums.StateCategory;

public interface IssueQueryRepository extends Repository<Issue, Long> {

	Optional<Issue> findById(Long id);

	Optional<Issue> findByKeyAndWorkspaceKey(String issueKey, String workspaceKey);

	Optional<Issue> findByKeyAndProject(String issueKey, Project project);

	List<Issue> findByKeyInAndWorkspaceKey(Collection<String> issueKeys, String workspaceKey);

	@Query("""
		    SELECT i
		    FROM Issue i
		    JOIN FETCH i.project p
		    JOIN FETCH i.issueType it
		    JOIN FETCH it.workflow
		    JOIN FETCH i.currentState
		    WHERE p.workspaceKey = :workspaceKey AND i.key = :issueKey
		""")
	Optional<Issue> findWithBasicInfo(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT i
		    FROM Issue i
		    JOIN FETCH i.project p
		    JOIN FETCH i.issueType it
		    JOIN FETCH i.currentState cs
		    LEFT JOIN FETCH i.participants.assignee a
		    LEFT JOIN FETCH a.workspaceMember awm
		    JOIN FETCH i.participants.reporter r
		    JOIN FETCH r.workspaceMember rwm
		    WHERE p.workspaceKey = :workspaceKey AND i.key = :issueKey
		""")
	Optional<Issue> findWithDetail(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT i
		    FROM Issue i
		    JOIN FETCH i.project p
		    LEFT JOIN FETCH i.parentIssue pi
		    LEFT JOIN FETCH pi.issueType pit
		    WHERE p.workspaceKey = :workspaceKey AND i.key = :issueKey
		""")
	Optional<Issue> findWithParent(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT child
		    FROM Issue child
		    JOIN FETCH child.issueType it
		    JOIN child.parentIssue pi
		    JOIN pi.project p
		    WHERE p.workspaceKey = :workspaceKey AND pi.key = :issueKey
		    ORDER BY child.createdAt ASC
		""")
	List<Issue> findChildren(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT COUNT(child) > 0
		    FROM Issue child
		    JOIN child.parentIssue pi
		    JOIN pi.project p
		    WHERE p.workspaceKey = :workspaceKey AND pi.key = :issueKey
		""")
	boolean hasChildren(
		@Param("workspaceKey") String workspaceKey,
		@Param("issueKey") String issueKey
	);

	boolean existsByIssueType(IssueType issueType);

	@Query("""
		    SELECT COALESCE(SUM(i.storyPoint), 0)
		    FROM Issue i
		    WHERE i.parentIssue.id = :parentId
		      AND i.softDeleted = false
		""")
	Integer sumChildrenStoryPoints(@Param("parentId") Long parentId);

	@Query("""
		    SELECT com.tissue.api.issue.application.dto.IssueCountStats(
		        COUNT(i),
		        SUM(CASE WHEN i.currentState.category = com.tissue.api.workflow.domain.enums.StateCategory.DONE THEN 1 ELSE 0 END)
		    )
		    FROM Issue i
		    WHERE i.parentIssue.id = :parentId
		      AND i.softDeleted = false
		""")
	IssueCountStats getChildIssueStats(@Param("parentId") Long parentId);

	@Query("""
		    SELECT new com.tissue.api.issue.application.dto.IssuePointStats(
		        COALESCE(SUM(i.storyPoint), 0),
		        COALESCE(SUM(CASE WHEN i.currentState.category = com.tissue.api.workflow.domain.enums.StateCategory.DONE
		        THEN i.storyPoint ELSE 0 END), 0)
		    )
		    FROM Issue i
		    WHERE i.parentIssue.id = :parentId
		      AND i.softDeleted = false
		""")
	IssuePointStats getChildPointStats(@Param("parentId") Long parentId);

	@Query("""
		    SELECT i FROM Issue i
		    WHERE i.sprint = :sprint
		      AND i.currentState.category != :doneCategory
		""")
	List<Issue> findIncompleteIssuesBySprint(
		@Param("sprint") Sprint sprint,
		@Param("doneCategory") StateCategory doneCategory
	);

	@Query("""
		    SELECT i.key
		    FROM Issue i
		    WHERE i.sprint = :sprint
		      AND i.currentState.category != :doneCategory
		""")
	List<String> findIncompleteIssueKeysBySprint(
		@Param("sprint") Sprint sprint,
		@Param("doneCategory") StateCategory doneCategory
	);

	@Query("""
		    SELECT i.key
		    FROM Issue i
		    WHERE i.sprint = :sprint
		""")
	List<String> findIssueKeysBySprint(@Param("sprint") Sprint sprint);

	@Query("""
		    SELECT COUNT(i) > 0
		    FROM Issue i
		    WHERE i.sprint = :sprint
		      AND i.currentState.category != :doneCategory
		""")
	boolean existsBySprintAndCategoryNot(
		@Param("sprint") Sprint sprint,
		@Param("doneCategory") StateCategory doneCategory
	);

	@Query("SELECT DISTINCT i.currentState.id " +
		"FROM Issue i " +
		"WHERE i.currentState.id IN :stateIds " +
		"AND i.softDeleted = false")
	List<Long> findStateIdsUsedByActiveIssues(@Param("stateIds") Collection<Long> stateIds);

	@Query("""
		    SELECT new com.tissue.api.issue.application.dto.IssueCountProjection(
		        i.currentState.id,
		        COUNT(i)
		    )
		    FROM Issue i
		    WHERE i.currentState.id IN :stateIds
		      AND i.softDeleted = false
		    GROUP BY i.currentState.id
		""")
	List<IssueCountProjection> findActiveIssueCounts(@Param("stateIds") Collection<Long> stateIds);
}
