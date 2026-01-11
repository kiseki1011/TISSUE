package com.tissue.notification.domain;

import com.tissue.common.entity.BaseDateEntity;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.vo.NotificationMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
public class ActivityLog extends BaseDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    //    @Embedded
    //    private EntityReference entityReference;

    @Embedded
    private NotificationMessage message;

    @Column(nullable = false)
    private Long actorMemberId;

    @SuppressWarnings("NullAway.Init")
    protected ActivityLog() {}

    // TODO: consider using statiuc factory method
    @Builder
    public ActivityLog(
            UUID eventId,
            NotificationType type,
            //            EntityReference entityReference,
            NotificationMessage message,
            Long actorMemberId) {
        this.eventId = eventId;
        this.type = type;
        //        this.entityReference = entityReference;
        this.message = message;
        this.actorMemberId = actorMemberId;
    }
}
