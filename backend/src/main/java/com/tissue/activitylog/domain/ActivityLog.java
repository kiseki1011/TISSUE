package com.tissue.activitylog.domain;

import com.tissue.activitylog.domain.enums.ActivityType;
import com.tissue.common.dto.FieldChange;
import com.tissue.common.entity.BaseDateEntity;
import com.tissue.common.jpa.converter.FieldChangeMapConverter;
import com.tissue.common.jpa.converter.StringListConverter;
import com.tissue.common.vo.EntityReference;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

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

    @Column(name = "activity_args", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    List<String> args = new ArrayList<>();

    @Column(name = "changes", columnDefinition = "TEXT")
    @Convert(converter = FieldChangeMapConverter.class)
    private Map<String, FieldChange> changes = new HashMap<>();

    @Column(name = "actor_member_id", nullable = false)
    private Long actorMemberId;

    @SuppressWarnings("NullAway.Init")
    protected ActivityLog() {}

    // TODO: consider using a static factory method instead of builder
    @Builder
    public ActivityLog(
            UUID eventId,
            ActivityType activityType,
            EntityReference entityReference,
            List<String> args,
            Map<String, FieldChange> changes,
            Long actorMemberId) {
        this.eventId = eventId;
        this.activityType = activityType;
        this.entityReference = entityReference;
        this.args = args;
        this.changes = changes;
        this.actorMemberId = actorMemberId;
    }
}
