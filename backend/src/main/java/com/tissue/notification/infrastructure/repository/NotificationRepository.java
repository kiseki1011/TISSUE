package com.tissue.notification.infrastructure.repository;

import com.tissue.notification.domain.model.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByReceiverMemberIdAndEntityReference_WorkspaceCodeAndIsReadFalse(
            Long memberId, String workspaceCode);

    Optional<Notification> findByIdAndReceiverMemberId(Long id, Long memberId);
}
