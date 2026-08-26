package com.tissue.feature.notification.application.port.usecase;

import com.tissue.feature.notification.application.dto.response.NotificationResponse;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.shared.dto.CursorPage;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface NotificationQueryUseCase {

    CursorPage<NotificationResponse> getNotifications(
            Long actorMemberId,
            boolean unreadOnly,
            @Nullable List<NotificationType> types,
            @Nullable String cursor,
            int limit);

    boolean checkUnreadStatus(Long actorMemberId);
}
