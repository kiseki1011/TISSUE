package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.domain.Issue;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueCommandRepository extends Repository<Issue, Long> {

    Issue save(Issue issue);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Issue i
            SET i.currentState.id = :toStateId
            WHERE i.currentState.id = :fromStateId
              AND i.softDeleted = false
            """)
    int bulkMigrateCurrentState(@Param("fromStateId") Long fromStateId, @Param("toStateId") Long toStateId);
}
