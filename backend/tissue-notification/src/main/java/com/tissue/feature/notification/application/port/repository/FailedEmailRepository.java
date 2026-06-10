package com.tissue.feature.notification.application.port.repository;

import com.tissue.feature.notification.domain.FailedEmail;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface FailedEmailRepository extends Repository<FailedEmail, Long> {

    FailedEmail save(FailedEmail failedEmail);

    void delete(FailedEmail failedEmail);

    List<FailedEmail> findAllByNextRetryAtBefore(Instant now);

    @Modifying(clearAutomatically = true)
    @Query(
            value = "DELETE FROM failed_email "
                    + "WHERE notification_id IN (SELECT id FROM notification WHERE project_key = :projectKey)",
            nativeQuery = true)
    void deleteByProjectKey(@Param("projectKey") String projectKey);
}
