package com.tissue.feature.activitylog.domain;

import com.tissue.feature.activitylog.domain.converter.FieldChangeMapConverter;
import com.tissue.shared.dto.FieldChange;
import com.tissue.shared.entity.BaseDateEntity;
import com.tissue.shared.vo.EntityReference;
import com.tissue.support.converter.StringMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
public class ActivityLog extends BaseDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    @Embedded
    private EntityReference entityReference;

    @Column(name = "activity_data", columnDefinition = "TEXT")
    @Convert(converter = StringMapConverter.class)
    private Map<String, String> data = new HashMap<>();

    @Column(name = "changes", columnDefinition = "TEXT")
    @Convert(converter = FieldChangeMapConverter.class)
    private Map<String, FieldChange> changes = new HashMap<>();

    @Column(name = "actor_member_id")
    @Nullable
    private Long actorMemberId;

    @SuppressWarnings("NullAway.Init")
    protected ActivityLog() {}

    @Builder
    public ActivityLog(
            UUID eventId,
            ActivityType activityType,
            EntityReference entityReference,
            Map<String, String> data,
            Map<String, FieldChange> changes,
            @Nullable Long actorMemberId) {

        this.eventId = eventId;
        this.activityType = activityType;
        this.entityReference = entityReference;
        this.data = data;
        this.changes = changes;
        this.actorMemberId = actorMemberId;
    }
}
