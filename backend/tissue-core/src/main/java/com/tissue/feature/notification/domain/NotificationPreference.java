package com.tissue.feature.notification.domain;

import com.tissue.feature.notification.domain.enums.NotificationChannel;
import com.tissue.feature.notification.domain.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(
        uniqueConstraints = {
            // Notifications are member-global: one preference row per member.
            @UniqueConstraint(
                    name = "UK_NOTIFICATION_PREF",
                    columnNames = {"receiver_member_id"})
        })
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_member_id", nullable = false)
    private Long receiverMemberId;

    /**
     * Store preferences as a native JSONB column.
     *
     * <p><strong>Data Structure Example:</strong>
     *
     * <pre>
     * {
     *  "EMAIL": {
     *      "ISSUE_ASSIGNED": true,
     *      "ISSUE_UPDATED": false,
     *  },
     *  "SLACK": {
     *      ...
     *   }
     * }
     * </pre>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", columnDefinition = "jsonb")
    private Map<String, Map<String, Boolean>> preferences = new HashMap<>();

    @SuppressWarnings("NullAway.Init")
    protected NotificationPreference() {}

    @Builder
    public NotificationPreference(Long receiverMemberId, Map<String, Map<String, Boolean>> preferences) {
        this.receiverMemberId = receiverMemberId;
        this.preferences = preferences != null ? preferences : new HashMap<>();
    }

    public void updatePreference(NotificationChannel channel, NotificationType type, boolean enabled) {
        preferences.computeIfAbsent(channel.name(), k -> new HashMap<>()).put(type.name(), enabled);
    }

    public boolean isEnabled(NotificationChannel channel, NotificationType type) {
        return preferences.getOrDefault(channel.name(), new HashMap<>()).getOrDefault(type.name(), true);
    }
}
