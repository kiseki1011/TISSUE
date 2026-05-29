package com.tissue.feature.issuetype.application.port.repository;

import com.tissue.feature.issuetype.domain.IssueType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueTypeRepository extends Repository<IssueType, Long> {

    IssueType save(IssueType issueType);

    void delete(IssueType issueType);

    Optional<IssueType> findById(Long id);

    @Query("""
           SELECT it
           FROM IssueType it
           JOIN FETCH it.workflow w
           JOIN FETCH w.initialState
           WHERE it.id = :issueTypeId
       """)
    Optional<IssueType> findWithWorkflowById(@Param("issueTypeId") Long issueTypeId);

    boolean existsByName_NormalizedName(String label);

    @Query("""
           SELECT it
           FROM IssueType it
           JOIN FETCH it.workflow w
           ORDER BY it.id ASC
       """)
    List<IssueType> findAllWithWorkflow();
}
