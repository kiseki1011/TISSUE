package com.tissue.notification.application.service;

import com.tissue.notification.domain.NotificationPreference;
import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.infrastructure.repository.NotificationPreferenceRepository;
import com.tissue.notification.presentation.dto.request.UpdateNotificationPreferenceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    @Transactional
    public void updatePreference(String workspaceKey, Long memberId, UpdateNotificationPreferenceRequest request) {
        NotificationPreference pref = findOrCreatePreference(workspaceKey, memberId, request.type(), request.channel());

        pref.updateEnabled(request.enabled());
        repository.save(pref);
    }

    private NotificationPreference findOrCreatePreference(
            String workspaceKey, Long memberId, NotificationType type, NotificationChannel channel) {
        return repository
                .findByReceiver(memberId, workspaceKey, type, channel)
                .orElseGet(() -> NotificationPreference.builder()
                        .receiverMemberId(memberId)
                        .workspaceKey(workspaceKey)
                        .type(type)
                        .channel(channel)
                        .enabled(true)
                        .build());
    }
}
