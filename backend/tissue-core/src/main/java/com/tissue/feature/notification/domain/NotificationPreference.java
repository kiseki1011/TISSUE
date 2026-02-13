package com.tissue.feature.notification.domain;

import com.tissue.feature.notification.domain.converter.PreferenceMapConverter;
import com.tissue.feature.notification.domain.enums.NotificationChannel;
import com.tissue.feature.notification.domain.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

@Entity
@Getter
@Table(
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "UK_NOTIFICATION_PREF",
                    columnNames = {"receiver_member_id", "workspace_key"})
        })
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_key", nullable = false)
    private String workspaceKey;

    @Column(name = "receiver_member_id", nullable = false)
    private Long receiverMemberId;

    // Map<ChannelName, Map<TypeName, Boolean>>
    @Column(name = "preferences", columnDefinition = "TEXT")
    @Convert(converter = PreferenceMapConverter.class)
    private Map<String, Map<String, Boolean>> preferences = new HashMap<>();

    @SuppressWarnings("NullAway.Init")
    protected NotificationPreference() {}

    @Builder
    public NotificationPreference(
            Long receiverMemberId, String workspaceKey, Map<String, Map<String, Boolean>> preferences) {
        this.receiverMemberId = receiverMemberId;
        this.workspaceKey = workspaceKey;
        this.preferences = preferences != null ? preferences : new HashMap<>();
    }

    public void updatePreference(NotificationChannel channel, NotificationType type, boolean enabled) {
        preferences.computeIfAbsent(channel.name(), k -> new HashMap<>()).put(type.name(), enabled);
    }

    /**
     * Default is "true" if not set
     */
    public boolean isEnabled(NotificationChannel channel, NotificationType type) {
        return preferences.getOrDefault(channel.name(), new HashMap<>()).getOrDefault(type.name(), true);
    }
}
