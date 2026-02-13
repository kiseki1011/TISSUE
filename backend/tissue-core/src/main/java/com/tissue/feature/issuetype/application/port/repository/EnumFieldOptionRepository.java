package com.tissue.feature.issuetype.application.port.repository;

import com.tissue.feature.issuetype.domain.EnumFieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface EnumFieldOptionRepository extends Repository<EnumFieldOption, Long> {

    EnumFieldOption save(EnumFieldOption option);

    List<EnumFieldOption> saveAll(Iterable<EnumFieldOption> options);

    void delete(EnumFieldOption enumFieldOption);

    Optional<EnumFieldOption> findByIdAndIssueField(Long id, IssueField field);

    List<EnumFieldOption> findByIssueFieldOrderByPositionAsc(IssueField field);

    @Query("""
       SELECT o
       FROM EnumFieldOption o
       JOIN FETCH o.issueField f
       JOIN FETCH f.issueType t
       JOIN FETCH t.project p
       WHERE o.id = :optionId
         AND f.id = :fieldId
         AND t.id = :typeId
         AND p.key = :projectKey
         AND p.workspaceKey = :workspaceKey
   """)
    Optional<EnumFieldOption> findWithHierarchyByKeys(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("typeId") Long typeId,
            @Param("fieldId") Long fieldId,
            @Param("optionId") Long optionId);

    boolean existsByIssueFieldAndName_Normalized(IssueField field, String label);

    int countByIssueField(IssueField field);

    @Query("select count(v) > 0 " + "from IssueFieldValue v " + "where v.enumOption = :option")
    boolean isInUse(@Param("option") EnumFieldOption option);
}
