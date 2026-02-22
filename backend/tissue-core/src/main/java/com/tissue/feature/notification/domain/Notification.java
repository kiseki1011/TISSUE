package com.tissue.feature.notification.domain;

import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.notification.domain.vo.NotificationMessage;
import com.tissue.shared.entity.BaseDateEntity;
import com.tissue.shared.enums.SupportedLanguage;
import com.tissue.shared.vo.EntityReference;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "UK_EVENT_RECEIVER",
                    columnNames = {"event_id", "receiver_member_id"})
        })
public class Notification extends BaseDateEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "receiver_member_id", nullable = false)
    private Long receiverMemberId;

    @Column(name = "receiver_email", nullable = false)
    private String receiverEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_language", nullable = false)
    private SupportedLanguage receiverLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType type;

    @Embedded
    private EntityReference entityReference;

    @Embedded
    private NotificationMessage message;

    @Nullable
    @Column(name = "actor_member_id")
    private Long actorMemberId;

    @Nullable
    @Column(name = "actor_display_name")
    private String actorDisplayName;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @SuppressWarnings("NullAway.Init")
    protected Notification() {}

    // TODO: consider using static factory method
    @Builder
    public Notification(
            UUID eventId,
            NotificationType notificationType,
            EntityReference entityReference,
            @Nullable Long actorMemberId,
            @Nullable String actorDisplayName,
            Long receiverMemberId,
            String receiverEmail,
            SupportedLanguage receiverLanguage,
            NotificationMessage message) {
        this.eventId = eventId;
        this.type = notificationType;
        this.entityReference = entityReference;
        this.actorMemberId = actorMemberId;
        this.actorDisplayName = actorDisplayName;
        this.receiverMemberId = receiverMemberId;
        this.receiverEmail = receiverEmail;
        this.receiverLanguage = receiverLanguage;
        this.message = message;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
