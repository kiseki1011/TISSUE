package com.tissue.feature.notification.adapter.event;

import com.tissue.feature.notification.domain.event.NotificationCreatedEvent;
import com.tissue.feature.realtime.application.RealtimeBroadcaster;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RealtimeNotificationEventBridge {

    private static final String NOTIFICATION_CATEGORY = "notification";
    private static final String NOTIFICATION_CREATED = "NOTIFICATION_CREATED";

    private final RealtimeBroadcaster broadcaster;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        broadcaster.broadcastToMembers(
                event.receiverMemberIds(),
                NOTIFICATION_CATEGORY,
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                Instant.now(),
                NOTIFICATION_CREATED,
                Map.of("notificationType", event.type().name()));
    }
}
