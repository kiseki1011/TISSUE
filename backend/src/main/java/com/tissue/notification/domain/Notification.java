package com.tissue.notification.domain;

import com.tissue.common.entity.BaseDateEntity;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.vo.EntityReference;
import com.tissue.notification.domain.vo.NotificationMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
@Table(
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "UK_EVENT_RECEIVER",
                    columnNames = {"eventId", "receiverMemberId"})
        })
public class Notification extends BaseDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private Long receiverMemberId;

    @Column(nullable = false)
    private String receiverEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Embedded
    private EntityReference entityReference;

    @Embedded
    private NotificationMessage message;

    @Column(nullable = false)
    private Long actorMemberId;

    private String actorDisplayName;

    @Column(nullable = false)
    private boolean isRead;

    @SuppressWarnings("NullAway.Init")
    protected Notification() {}

    // TODO: consider using static factory method
    @Builder
    public Notification(
            UUID eventId,
            NotificationType notificationType,
            EntityReference entityReference,
            Long actorMemberId,
            String actorDisplayName,
            Long receiverMemberId,
            String receiverEmail,
            NotificationMessage message) {
        this.eventId = eventId;
        this.type = notificationType;
        this.entityReference = entityReference;
        this.actorMemberId = actorMemberId;
        this.actorDisplayName = actorDisplayName;
        this.receiverMemberId = receiverMemberId;
        this.receiverEmail = receiverEmail;
        this.message = message;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
