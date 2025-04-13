package com.tissue.api.notification.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.notification.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByReceiverWorkspaceMemberIdAndEntityReference_WorkspaceCodeAndIsReadFalse(
		Long receiverWorkspaceMemberId,
		String workspaceCode
	);

	Optional<Notification> findByIdAndReceiverWorkspaceMemberId(
		Long id,
		Long receiverWorkspaceMemberId
	);
}
