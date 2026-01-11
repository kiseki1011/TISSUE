package com.tissue.notification.application.service;

import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.NotificationPreference;
import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.service.sender.NotificationSender;
import com.tissue.notification.infrastructure.repository.NotificationPreferenceRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessor {

    private final List<NotificationSender> senders;
    private final NotificationPreferenceRepository preferenceRepository;

    @Async
    public void process(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }

        Notification context = notifications.get(0);
        String workspaceKey = context.getEntityReference().getWorkspaceKey();
        var type = context.getType();
        List<Long> receiverIds =
                notifications.stream().map(Notification::getReceiverMemberId).toList();

        // bulk load preferences
        List<NotificationPreference> preferences =
                preferenceRepository.findByWorkspaceKeyAndTypeAndReceiverMemberIdIn(workspaceKey, type, receiverIds);

        // build lookup map: receiverId -> channel -> enabled
        Map<Long, Map<NotificationChannel, Boolean>> prefMap = preferences.stream()
                .collect(Collectors.groupingBy(
                        NotificationPreference::getReceiverMemberId,
                        Collectors.toMap(NotificationPreference::getChannel, NotificationPreference::isEnabled)));

        for (NotificationSender sender : senders) {
            NotificationChannel channel = sender.getChannel();
            if (channel == NotificationChannel.IN_APP) {
                continue;
            }

            try {
                List<Notification> targets = notifications.stream()
                        .filter(n -> isChannelEnabled(n.getReceiverMemberId(), channel, prefMap))
                        .toList();

                if (!targets.isEmpty()) {
                    // TODO: Consider batch send interface for Senders
                    targets.forEach(sender::send);
                }
            } catch (Exception e) {
                log.error("Failed to process notification channel: {}", channel, e);
            }
        }
    }

    private boolean isChannelEnabled(
            Long memberId, NotificationChannel channel, Map<Long, Map<NotificationChannel, Boolean>> prefMap) {
        Map<NotificationChannel, Boolean> memberPrefs = prefMap.getOrDefault(memberId, Collections.emptyMap());
        return memberPrefs.getOrDefault(channel, true);
    }
}
