package com.tissue.feature.notification.application.port.usecase;

import com.tissue.feature.notification.application.dto.response.NotificationResponse;
import com.tissue.shared.dto.KeysetPageResponse;
import org.jspecify.annotations.Nullable;

public interface NotificationQueryUseCase {

    KeysetPageResponse<NotificationResponse> getNotifications(
            Long actorMemberId, boolean unreadOnly, @Nullable Long keysetId, int limit);

    boolean checkUnreadStatus(Long actorMemberId);
}
