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

    List<IssueType> saveAll(Iterable<IssueType> issueTypes);

    void delete(IssueType issueType);

    Optional<IssueType> findByIdAndProject(Long id, Project project);

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

    boolean existsByName_NormalizedAndProject(String label, Project project);
}
