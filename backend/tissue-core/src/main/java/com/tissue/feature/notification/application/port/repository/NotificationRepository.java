package com.tissue.feature.notification.application.port.repository;

import com.tissue.feature.notification.domain.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends Repository<Notification, Long> {

    Notification save(Notification notification);

    List<Notification> saveAll(Iterable<Notification> notifications);

    Optional<Notification> findById(Long id);

    List<Notification> findAll();

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

    @Query("SELECT n FROM Notification n "
            + "WHERE n.receiverMemberId = :memberId "
            + "AND n.entityReference.workspaceKey = :workspaceKey "
            + "AND (:cursorId IS NULL OR n.id < :cursorId) "
            + "ORDER BY n.id DESC")
    List<Notification> findByCursor(
            @Param("memberId") Long memberId,
            @Param("workspaceKey") String workspaceKey,
            @org.jspecify.annotations.Nullable @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("SELECT n FROM Notification n "
            + "WHERE n.receiverMemberId = :memberId "
            + "AND n.entityReference.workspaceKey = :workspaceKey "
            + "AND n.isRead = false "
            + "AND (:cursorId IS NULL OR n.id < :cursorId) "
            + "ORDER BY n.id DESC")
    List<Notification> findUnreadByCursor(
            @Param("memberId") Long memberId,
            @Param("workspaceKey") String workspaceKey,
            @org.jspecify.annotations.Nullable @Param("cursorId") Long cursorId,
            Pageable pageable);
}
