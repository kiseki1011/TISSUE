package com.tissue.notification.infrastructure.repository;

import com.tissue.notification.domain.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByReceiverMemberIdAndEntityReference_WorkspaceKeyAndIsReadFalse(
            Long memberId, String workspaceKey);

    List<Notification> findByReceiverMemberIdAndEntityReference_WorkspaceKeyOrderByCreatedAtDesc(
            Long memberId, String workspaceKey);

    Optional<Notification> findByIdAndReceiverMemberId(Long id, Long memberId);
}
