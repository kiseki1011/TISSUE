package com.tissue.notification.application.service;

import com.tissue.notification.adapter.web.request.UpdateNotificationPreferenceRequest;
import com.tissue.notification.application.dto.response.NotificationPreferenceResponse;
import com.tissue.notification.application.port.out.NotificationPreferenceRepository;
import com.tissue.notification.domain.NotificationPreference;
import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;
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
    public void updatePreference(String workspaceKey, Long memberId, UpdateNotificationPreferenceRequest request) {
        NotificationPreference pref = preferenceRepository
                .findByReceiverMemberIdAndWorkspaceKey(memberId, workspaceKey)
                .orElseGet(() -> NotificationPreference.builder()
                        .receiverMemberId(memberId)
                        .workspaceKey(workspaceKey)
                        .build());

        pref.updatePreference(request.channel(), request.type(), request.enabled());
        preferenceRepository.save(pref);
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getPreferences(String workspaceKey, Long memberId) {
        NotificationPreference preference = preferenceRepository
                .findByReceiverMemberIdAndWorkspaceKey(memberId, workspaceKey)
                .orElse(null);

        List<NotificationPreferenceResponse> responses = new ArrayList<>();

        for (NotificationType type : NotificationType.values()) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                if (channel == NotificationChannel.IN_APP) {
                    continue;
                }

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
