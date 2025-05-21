package com.tissue.api.notification.domain.service.sender.email;

import org.springframework.stereotype.Component;

import com.tissue.api.notification.domain.enums.NotificationChannel;
import com.tissue.api.notification.domain.model.Notification;
import com.tissue.api.notification.domain.service.sender.EmailClient;
import com.tissue.api.notification.domain.service.sender.NotificationSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailSender implements NotificationSender {

	private final EmailClient emailClient;

	@Override
	public NotificationChannel getChannel() {
		return NotificationChannel.EMAIL;
	}

	@Override
	public void send(Notification notification) {
		// TODO: EmailClient를 사용한 실제 알림 이메일 전송 작업
		// TODO: try-catch로 예외 잡고 로깅
		// TODO: 수신자(receiver)의 이메일 필드를 Notification에 추가하는게 좋읗 듯(여기서 조회하기 싫음)
		// TODO: 최선 노력 방식(best effort)이어도 Exception을 잡는게 좋은 방식인가?
		//  일단 발생 가능성이 더 높은 세세한 예외를 위에서 잡아야 하는건 알겠음
		try {
			// String to = notification.getReceiverEmail();
			String subject = notification.getTitle();
			String body = notification.getContent();

			emailClient.send("email@placeholder", subject, body);
		} catch (Exception e) {
			log.warn("failed to send email notification: receiver member id={}, title={}, cause={}",
				notification.getReceiverMemberId(),
				notification.getTitle(),
				e.getMessage(),
				e);
		}
	}
}
