package com.tissue.notification.application.port.out;

import com.tissue.notification.domain.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends Repository<Notification, Long> {

    Notification save(Notification notification);

    List<Notification> saveAll(Iterable<Notification> notifications);

    Optional<Notification> findById(Long id);

    List<Notification> findByReceiverMemberIdAndEntityReference_WorkspaceKeyAndIsReadFalse(
            Long memberId, String workspaceKey);

    List<Notification> findByReceiverMemberIdAndEntityReference_WorkspaceKeyOrderByCreatedAtDesc(
            Long memberId, String workspaceKey);

    boolean existsByReceiverMemberIdAndEntityReference_WorkspaceKeyAndIsReadFalse(Long memberId, String workspaceKey);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true "
            + "WHERE n.receiverMemberId = :memberId "
            + "AND n.entityReference.workspaceKey = :workspaceKey "
            + "AND n.isRead = false")
    void markAllAsRead(@Param("memberId") Long memberId, @Param("workspaceKey") String workspaceKey);
}
