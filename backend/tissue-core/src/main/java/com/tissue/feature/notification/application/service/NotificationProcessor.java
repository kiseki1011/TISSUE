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

    // TODO: consider using different executors for each sender.
    //  the needed back-pressures may vary
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
        NotificationType type = context.getNotificationType();
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
