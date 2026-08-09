package com.tissue.feature.vcs.application.port.repository;

import com.tissue.feature.vcs.domain.VcsWebhookDelivery;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.enums.WebhookDeliveryStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface VcsWebhookDeliveryRepository extends Repository<VcsWebhookDelivery, Long> {

    VcsWebhookDelivery save(VcsWebhookDelivery delivery);

    Optional<VcsWebhookDelivery> findById(Long id);

    @Query("""
            SELECT d.id
            FROM VcsWebhookDelivery d
            WHERE d.status = :status
              AND d.nextAttemptAt <= :now
            ORDER BY d.nextAttemptAt ASC
        """)
    List<Long> findDueForRetry(@Param("status") WebhookDeliveryStatus status, @Param("now") Instant now, Limit limit);

    Page<VcsWebhookDelivery> findByProjectKeyAndProvider(String projectKey, VcsProvider provider, Pageable pageable);

    @Modifying
    @Query("DELETE FROM VcsWebhookDelivery d WHERE d.createdAt < :threshold")
    int deleteOlderThan(@Param("threshold") Instant threshold);
}
