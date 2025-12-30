package com.tissue.notification.infrastructure.repository;

import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.model.NotificationPreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    @Query("""
            SELECT p FROM NotificationPreference p
            WHERE p.receiverMemberId = :memberId
            AND p.workspaceKey = :workspaceKey
            AND p.type = :type
            AND p.channel = :channel
            """)
    Optional<NotificationPreference> findByReceiver(
            @Param("memberId") Long memberId,
            @Param("workspaceKey") String workspaceCode,
            @Param("type") NotificationType type,
            @Param("channel") NotificationChannel channel);
}
