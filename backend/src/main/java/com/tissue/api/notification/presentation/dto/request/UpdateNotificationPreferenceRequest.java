package com.tissue.api.notification.presentation.dto.request;

import com.tissue.api.notification.domain.enums.NotificationType;

import jakarta.validation.constraints.NotNull;

// TODO: 추후에 notification channel에 따른 설정도 제공할 필요가 있음(Slack, Discord, 등...을 지원한다면)
//  지금은 그냥 email의 알림 설정만 가능(inApp은 기본적으로 항상 true지만 이것도 변경 예정)
public record UpdateNotificationPreferenceRequest(
	@NotNull(message = "{valid.notnull}")
	NotificationType type,

	@NotNull(message = "{valid.notnull}")
	boolean enabled
) {
}
