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

    // TODO: emailExecutor는 가상 스레드를 사용하는 ThrottledExecutor를 사용함. 백프레셔는 세마포어를 사용.
    //  SlackSender 등의 기타 sender들은 다른 executor를 사용하도록 설정하도록 하는게 좋을 것 같음.
    //  왜냐하면 emailExecutor의 세마포어는 email 전용으로 맞춰져 있기 때문.
    //  성능을 테스트해서 적당한 값으로 sender 별로 설정하자(sender 별로 적당한 executor를 매핑).
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
