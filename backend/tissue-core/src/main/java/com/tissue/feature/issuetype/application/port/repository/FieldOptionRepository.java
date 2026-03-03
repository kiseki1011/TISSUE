package com.tissue.feature.issuetype.application.port.repository;

import com.tissue.feature.issuetype.domain.FieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface FieldOptionRepository extends Repository<FieldOption, Long> {

    FieldOption save(FieldOption option);

    List<FieldOption> findAllById(Iterable<Long> ids);

    void delete(FieldOption fieldOption);

    Optional<FieldOption> findByIdAndIssueField(Long id, IssueField field);

    @Query("""
       SELECT o
       FROM FieldOption o
       JOIN FETCH o.issueField f
       JOIN FETCH f.issueType t
       JOIN FETCH t.project p
       WHERE o.id = :optionId
         AND f.id = :fieldId
         AND t.id = :typeId
         AND p.key = :projectKey
         AND p.workspaceKey = :workspaceKey
   """)
    Optional<FieldOption> findWithHierarchyByKeys(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("typeId") Long typeId,
            @Param("fieldId") Long fieldId,
            @Param("optionId") Long optionId);

    boolean existsByIssueFieldAndName_Normalized(IssueField field, String label);

    @Query("select count(v) > 0 " + "from IssueFieldValue v " + "where v.fieldOption = :option")
    boolean isInUse(@Param("option") FieldOption option);
}
