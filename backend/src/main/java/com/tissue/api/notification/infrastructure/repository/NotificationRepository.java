package com.tissue.api.notification.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.notification.domain.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByReceiverMemberIdAndEntityReference_WorkspaceCodeAndIsReadFalse(
		Long memberId,
		String workspaceCode
	);

	Optional<Notification> findByIdAndReceiverMemberId(
		Long id,
		Long memberId
	);
}
