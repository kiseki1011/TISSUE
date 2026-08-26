package com.tissue.feature.project.application.port.repository;

import com.tissue.feature.project.domain.Project;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Native, FK-safe bulk deletes for permanently purging a project aggregate.
 *
 * <p>Native SQL is mandatory: {@code @SQLRestriction("soft_deleted = false")} on Project/Issue/Sprint hides the
 * (already soft-deleted) target rows from JPA finders, Specifications and managed loads, so JPQL/derived deletes
 * would touch 0 rows. There is no JPA cascade and no DB {@code ON DELETE CASCADE} (ddl-auto), so every child set is
 * removed explicitly in child-to-parent order by {@code ProjectHardDeleteService}.
 */
@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.NOT_REVIEWED,
        evaluationReason = "Passes AI written integration test, needs review.",
        agentName = "claude-opus-4-8")
public interface ProjectPurgeRepository extends Repository<Project, Long> {

    /*--- Read (preview / report / file collection) — must run before deletes --- */

    @Query(
            value = "SELECT stored_path FROM issue_attachment "
                    + "WHERE issue_id IN (SELECT id FROM issue WHERE project_id = :projectId)",
            nativeQuery = true)
    List<String> findStoredPaths(@Param("projectId") Long projectId);

    @Query(value = "SELECT COUNT(*) FROM issue WHERE project_id = :projectId", nativeQuery = true)
    long countIssues(@Param("projectId") Long projectId);

    @Query(
            value = "SELECT COUNT(*) FROM comment "
                    + "WHERE issue_id IN (SELECT id FROM issue WHERE project_id = :projectId)",
            nativeQuery = true)
    long countComments(@Param("projectId") Long projectId);

    @Query(
            value = "SELECT COUNT(*) FROM issue_attachment "
                    + "WHERE issue_id IN (SELECT id FROM issue WHERE project_id = :projectId)",
            nativeQuery = true)
    long countAttachments(@Param("projectId") Long projectId);

    @Query(value = "SELECT COUNT(*) FROM sprint WHERE project_id = :projectId", nativeQuery = true)
    long countSprints(@Param("projectId") Long projectId);

    @Query(value = "SELECT COUNT(*) FROM tag WHERE project_id = :projectId", nativeQuery = true)
    long countTags(@Param("projectId") Long projectId);

    @Query(value = "SELECT COUNT(*) FROM project_member WHERE project_id = :projectId", nativeQuery = true)
    long countMembers(@Param("projectId") Long projectId);

    @Query(value = "SELECT COUNT(*) FROM activity_log WHERE project_key = :projectKey", nativeQuery = true)
    long countActivityLogs(@Param("projectKey") String projectKey);

    @Query(value = "SELECT COUNT(*) FROM project_vcs_integration WHERE project_key = :projectKey", nativeQuery = true)
    long countVcsIntegrations(@Param("projectKey") String projectKey);

    /* --- Issue subtree — must run while issue rows still exist (subqueries) --- */

    /**
     * Relations can cross project boundaries, so both endpoints are matched to avoid a dangling FK elsewhere.
     */
    @Modifying(clearAutomatically = true)
    @Query(
            value = "DELETE FROM issue_relation "
                    + "WHERE source_issue_id IN (SELECT id FROM issue WHERE project_id = :projectId) "
                    + "OR target_issue_id IN (SELECT id FROM issue WHERE project_id = :projectId)",
            nativeQuery = true)
    void deleteIssueRelations(@Param("projectId") Long projectId);

    @Modifying(clearAutomatically = true)
    @Query(
            value = "DELETE FROM issue_attachment "
                    + "WHERE issue_id IN (SELECT id FROM issue WHERE project_id = :projectId)",
            nativeQuery = true)
    void deleteAttachments(@Param("projectId") Long projectId);

    @Modifying(clearAutomatically = true)
    @Query(
            value = "DELETE FROM issue_branch "
                    + "WHERE issue_id IN (SELECT id FROM issue WHERE project_id = :projectId)",
            nativeQuery = true)
    void deleteIssueBranches(@Param("projectId") Long projectId);

    @Modifying(clearAutomatically = true)
    @Query(
            value = "DELETE FROM issue_pull_request "
                    + "WHERE issue_id IN (SELECT id FROM issue WHERE project_id = :projectId)",
            nativeQuery = true)
    void deleteIssuePullRequests(@Param("projectId") Long projectId);

    @Modifying(clearAutomatically = true)
    @Query(
            value = "DELETE FROM issue_reviewer "
                    + "WHERE issue_id IN (SELECT id FROM issue WHERE project_id = :projectId)",
            nativeQuery = true)
    void deleteIssueReviewers(@Param("projectId") Long projectId);

    @Modifying(clearAutomatically = true)
    @Query(
            value = "DELETE FROM issue_subscriber "
                    + "WHERE issue_id IN (SELECT id FROM issue WHERE project_id = :projectId)",
            nativeQuery = true)
    void deleteIssueSubscribers(@Param("projectId") Long projectId);

    @Modifying(clearAutomatically = true)
    @Query(
            value = "DELETE FROM issue_tag " + "WHERE issue_id IN (SELECT id FROM issue WHERE project_id = :projectId)",
            nativeQuery = true)
    void deleteIssueTags(@Param("projectId") Long projectId);

    @Modifying(clearAutomatically = true)
    @Query(
            value = "DELETE FROM comment " + "WHERE issue_id IN (SELECT id FROM issue WHERE project_id = :projectId)",
            nativeQuery = true)
    void deleteComments(@Param("projectId") Long projectId);

    /**
     * Wiki documents are global; only the dangling polymorphic links to this project/its issues go.
     */
    @Modifying(clearAutomatically = true)
    @Query(
            value = "DELETE FROM wiki_link "
                    + "WHERE (target_type = 'ISSUE' "
                    + "AND target_id IN (SELECT id FROM issue WHERE project_id = :projectId)) "
                    + "OR (target_type = 'PROJECT' AND target_id = :projectId)",
            nativeQuery = true)
    void deleteDanglingWikiLinks(@Param("projectId") Long projectId);

    /**
     * Surviving (possibly cross-project) children must drop their parent link before the parent rows are deleted.
     */
    @Modifying(clearAutomatically = true)
    @Query(
            value = "UPDATE issue SET parent_issue_id = NULL "
                    + "WHERE parent_issue_id IN (SELECT id FROM issue WHERE project_id = :projectId)",
            nativeQuery = true)
    void nullifyParentIssueReferences(@Param("projectId") Long projectId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM issue WHERE project_id = :projectId", nativeQuery = true)
    void deleteIssues(@Param("projectId") Long projectId);

    /* --- Project-direct children --- */

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM tag WHERE project_id = :projectId", nativeQuery = true)
    void deleteTags(@Param("projectId") Long projectId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM sprint WHERE project_id = :projectId", nativeQuery = true)
    void deleteSprints(@Param("projectId") Long projectId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM project_member WHERE project_id = :projectId", nativeQuery = true)
    void deleteProjectMembers(@Param("projectId") Long projectId);

    /* --- Denormalized / cross-module orphans (no FK, keyed by project_key) --- */

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM project_vcs_integration WHERE project_key = :projectKey", nativeQuery = true)
    void deleteVcsIntegrations(@Param("projectKey") String projectKey);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM activity_log WHERE project_key = :projectKey", nativeQuery = true)
    void deleteActivityLogs(@Param("projectKey") String projectKey);

    /* --- The project row itself — last --- */

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM project WHERE id = :projectId", nativeQuery = true)
    void deleteProject(@Param("projectId") Long projectId);
}
