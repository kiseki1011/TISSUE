package com.tissue.notification.application.port.out;

import com.tissue.notification.domain.Notification;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface NotificationRepository extends Repository<Notification, Long> {

    List<Notification> saveAll(Iterable<Notification> notifications);

    List<Notification> findByReceiverMemberIdAndEntityReference_WorkspaceKeyAndIsReadFalse(
            Long memberId, String workspaceKey);

    List<Notification> findByReceiverMemberIdAndEntityReference_WorkspaceKeyOrderByCreatedAtDesc(
            Long memberId, String workspaceKey);

    boolean existsByReceiverMemberIdAndEntityReference_WorkspaceKeyAndIsReadFalse(Long memberId, String workspaceKey);
}
