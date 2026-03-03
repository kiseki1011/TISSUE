package com.tissue.feature.notification.application.service;

import com.tissue.feature.notification.application.port.repository.NotificationPreferenceRepository;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.NotificationPreference;
import com.tissue.feature.notification.domain.enums.NotificationChannel;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.notification.domain.service.NotificationSender;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessor {

    private final List<NotificationSender> senders;
    private final NotificationPreferenceRepository preferenceRepository;

    public void process(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }

        Notification context = notifications.get(0);
        String workspaceKey = context.getEntityReference().getWorkspaceKey();
        NotificationType type = context.getNotificationType();
        List<Long> receiverIds =
                notifications.stream().map(Notification::getReceiverMemberId).toList();

        List<NotificationPreference> preferences =
                preferenceRepository.findAllByWorkspaceKeyAndReceiverMemberIdIn(workspaceKey, receiverIds);

        Map<Long, NotificationPreference> prefMap = preferences.stream()
                .collect(Collectors.toMap(NotificationPreference::getReceiverMemberId, Function.identity()));

        for (NotificationSender sender : senders) {
            NotificationChannel channel = sender.getChannel();
            if (channel == NotificationChannel.IN_APP) {
                continue;
            }

            List<Notification> targets = notifications.stream()
                    .filter(n -> isChannelEnabled(n.getReceiverMemberId(), channel, type, prefMap))
                    .toList();

            if (targets.isEmpty()) {
                continue;
            }

            for (Notification target : targets) {
                sender.getExecutor().execute(() -> sender.send(target));
            }
        }
    }

    private boolean isChannelEnabled(
            Long memberId,
            NotificationChannel channel,
            NotificationType type,
            Map<Long, NotificationPreference> preferenceMap) {

        return Optional.ofNullable(preferenceMap.get(memberId))
                .map(p -> p.isEnabled(channel, type))
                .orElse(true);
    }
}
