package com.tissue.api.notification.application.service.command;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tissue.api.notification.domain.enums.NotificationChannel;
import com.tissue.api.notification.domain.model.Notification;
import com.tissue.api.notification.domain.model.NotificationPreference;
import com.tissue.api.notification.domain.service.sender.NotificationSender;
import com.tissue.api.notification.infrastructure.repository.NotificationPreferenceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessor {

	private final List<NotificationSender> senders;
	private final NotificationPreferenceRepository preferenceRepository;

	public void process(Notification notification) {
		for (NotificationSender sender : senders) {
			if (shouldSend(notification, sender.getChannel())) {
				sender.send(notification);
			}
		}
	}

	private boolean shouldSend(Notification notification, NotificationChannel channel) {
		return preferenceRepository.findByReceiver(
				notification.getReceiverMemberId(),
				notification.getEntityReference().getWorkspaceCode(),
				notification.getType(),
				channel
			)
			.map(NotificationPreference::isEnabled)
			.orElse(true); // 설정 없으면 수신 허용
	}
}
