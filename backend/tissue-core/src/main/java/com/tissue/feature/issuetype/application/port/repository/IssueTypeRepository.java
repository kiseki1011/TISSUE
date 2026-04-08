package com.tissue.feature.issuetype.application.port.repository;

import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.domain.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueTypeRepository extends Repository<IssueType, Long> {

    IssueType save(IssueType issueType);

    void delete(IssueType issueType);

    @Query("""
           SELECT it
           FROM IssueType it
           JOIN FETCH it.project p
           WHERE it.id = :issueTypeId
             AND p.key = :projectKey
             AND p.workspaceKey = :workspaceKey
       """)
    Optional<IssueType> findWithProjectByWorkspaceKeyAndProjectKeyAndId(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("issueTypeId") Long issueTypeId);

    @Query("""
           SELECT it
           FROM IssueType it
           JOIN FETCH it.project p
           JOIN FETCH it.workflow w
           JOIN FETCH w.initialState
           WHERE it.id = :issueTypeId
             AND p.key = :projectKey
             AND p.workspaceKey = :workspaceKey
       """)
    Optional<IssueType> findWithProjectAndWorkflowByWorkspaceKeyAndProjectKeyAndId(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("issueTypeId") Long issueTypeId);

    @Query("""
           SELECT it
           FROM IssueType it
           JOIN FETCH it.project p
           WHERE it.id = :issueTypeId
             AND p.workspaceKey = :workspaceKey
       """)
    Optional<IssueType> findWithProjectByWorkspaceKeyAndId(
            @Param("workspaceKey") String workspaceKey, @Param("issueTypeId") Long issueTypeId);

    boolean existsByName_NormalizedNameAndProject(String label, Project project);

    @Query("""
           SELECT it
           FROM IssueType it
           JOIN FETCH it.workflow w
           JOIN it.project p
           WHERE p.workspaceKey = :workspaceKey
             AND it.id IN :ids
       """)
    List<IssueType> findAllWithWorkflowByWorkspaceKeyAndIdIn(
            @Param("workspaceKey") String workspaceKey, @Param("ids") List<Long> ids);
}
