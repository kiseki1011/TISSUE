package com.tissue.notification.application.service;

import com.tissue.notification.application.port.out.NotificationPreferenceRepository;
import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.NotificationPreference;
import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.service.NotificationSender;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationProcessor {

    private final List<NotificationSender> senders;
    private final NotificationPreferenceRepository preferenceRepository;
    private final Executor emailExecutor;

    public NotificationProcessor(
            List<NotificationSender> senders,
            NotificationPreferenceRepository preferenceRepository,
            @Qualifier("emailExecutor") Executor emailExecutor) {
        this.senders = senders;
        this.preferenceRepository = preferenceRepository;
        this.emailExecutor = emailExecutor;
    }

    public void process(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }

        Notification context = notifications.get(0);
        String workspaceKey = context.getEntityReference().getWorkspaceKey();
        NotificationType type = context.getType();
        List<Long> receiverIds =
                notifications.stream().map(Notification::getReceiverMemberId).toList();

        List<NotificationPreference> preferences =
                preferenceRepository.findAllByWorkspaceKeyAndReceiverMemberIdIn(workspaceKey, receiverIds);

        // receiverId -> Preference Entity
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
                emailExecutor.execute(() -> sender.send(target));
            }
        }
    }

    private boolean isChannelEnabled(
            Long memberId,
            NotificationChannel channel,
            NotificationType type,
            Map<Long, NotificationPreference> preferanceMap) {
        return Optional.ofNullable(preferanceMap.get(memberId))
                .map(p -> p.isEnabled(channel, type))
                .orElse(true);
    }
}
