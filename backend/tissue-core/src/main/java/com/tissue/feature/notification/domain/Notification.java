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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
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
    private NotificationType notificationType;

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

    public static Notification create(
            UUID eventId,
            NotificationType type,
            EntityReference reference,
            Long receiverMemberId,
            String receiverEmail,
            SupportedLanguage receiverLanguage,
            NotificationMessage message,
            @Nullable Long actorMemberId,
            @Nullable String actorDisplayName) {

        return Notification.builder()
                .eventId(eventId)
                .notificationType(type)
                .entityReference(reference)
                .receiverMemberId(receiverMemberId)
                .receiverEmail(receiverEmail)
                .receiverLanguage(receiverLanguage)
                .message(message)
                .actorMemberId(actorMemberId)
                .actorDisplayName(actorDisplayName)
                .build();
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
