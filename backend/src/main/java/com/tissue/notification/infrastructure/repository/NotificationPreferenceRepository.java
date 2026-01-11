package com.tissue.notification.infrastructure.repository;

import com.tissue.notification.domain.NotificationPreference;
import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface NotificationPreferenceRepository extends Repository<NotificationPreference, Long> {

    NotificationPreference save(NotificationPreference preference);

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

    @Query("""
            SELECT p FROM NotificationPreference p
            WHERE p.workspaceKey = :workspaceKey
            AND p.type = :type
            AND p.receiverMemberId IN :memberIds
            """)
    List<NotificationPreference> findByWorkspaceKeyAndTypeAndReceiverMemberIdIn(
            @Param("workspaceKey") String workspaceKey,
            @Param("type") NotificationType type,
            @Param("memberIds") Collection<Long> memberIds);
}
