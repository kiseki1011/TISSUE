package com.tissue.feature.notification.application.port.usecase;

import com.tissue.feature.notification.application.dto.response.NotificationResponse;
import com.tissue.shared.dto.CursorPage;
import org.jspecify.annotations.Nullable;

public interface NotificationQueryUseCase {

    CursorPage<NotificationResponse> getNotifications(
            Long actorMemberId, boolean unreadOnly, @Nullable String cursor, int limit);

    boolean checkUnreadStatus(Long actorMemberId);
}
