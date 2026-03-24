package com.tissue.feature.notification.application.service;

import com.tissue.feature.notification.application.dto.request.UpdateNotificationPreferenceCommand;
import com.tissue.feature.notification.application.dto.response.NotificationPreferenceResponse;
import com.tissue.feature.notification.application.port.repository.NotificationPreferenceRepository;
import com.tissue.feature.notification.domain.NotificationPreference;
import com.tissue.feature.notification.domain.enums.NotificationChannel;
import com.tissue.feature.notification.domain.enums.NotificationType;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional
    public void updatePreference(String workspaceKey, UpdateNotificationPreferenceCommand cmd, Long actorMemberId) {
        NotificationPreference pref = preferenceRepository
                .findByWorkspaceKeyAndReceiverMemberId(workspaceKey, actorMemberId)
                .orElseGet(() -> NotificationPreference.builder()
                        .receiverMemberId(actorMemberId)
                        .workspaceKey(workspaceKey)
                        .build());

        pref.updatePreference(cmd.channel(), cmd.type(), cmd.enabled());
        preferenceRepository.save(pref);
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getPreferences(String workspaceKey, Long actorMemberId) {
        NotificationPreference preference = preferenceRepository
                .findByWorkspaceKeyAndReceiverMemberId(workspaceKey, actorMemberId)
                .orElse(null);

        List<NotificationPreferenceResponse> responses = new ArrayList<>();

        for (NotificationType type : NotificationType.values()) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                boolean enabled = true;
                if (preference != null) {
                    enabled = preference.isEnabled(channel, type);
                }

                responses.add(NotificationPreferenceResponse.builder()
                        .type(type)
                        .channel(channel)
                        .enabled(enabled)
                        .build());
            }
        }
        return responses;
    }
}
