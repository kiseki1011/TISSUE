package com.tissue.api.notification.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.notification.domain.enums.NotificationChannel;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.model.NotificationPreference;

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
		@Param("channel") NotificationChannel channel
	);
}
