package com.tissue.api.notification.domain.service.sender.email;

import org.springframework.stereotype.Component;

import com.tissue.api.notification.domain.enums.NotificationChannel;
import com.tissue.api.notification.domain.model.Notification;
import com.tissue.api.notification.domain.service.sender.NotificationSender;

@Component
public class EmailSender implements NotificationSender {

	// TODO: 이메일 클라이언트 인터페이스와 구현체 추가
	// private final EmailClient emailClient;

	@Override
	public NotificationChannel getChannel() {
		return NotificationChannel.EMAIL;
	}

	@Override
	public void send(Notification notification) {
		// TODO: EmailClient를 사용한 실제 알림 이메일 전송 작업
		// TODO: try-catch로 예외 잡고 로깅
	}
}
