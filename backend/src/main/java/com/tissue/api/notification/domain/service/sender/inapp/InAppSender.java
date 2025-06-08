package com.tissue.api.notification.domain.service.sender.inapp;

import org.springframework.stereotype.Component;

import com.tissue.api.notification.domain.enums.NotificationChannel;
import com.tissue.api.notification.domain.model.Notification;
import com.tissue.api.notification.domain.service.sender.NotificationSender;

@Component
public class InAppSender implements NotificationSender {

	@Override
	public NotificationChannel getChannel() {
		return NotificationChannel.IN_APP;
	}

	@Override
	public void send(Notification notification) {
		// TODO: 인앱 알림은 DB 저장만으로 충분, 필요하면 추가적인 작업 수행
	}
}
