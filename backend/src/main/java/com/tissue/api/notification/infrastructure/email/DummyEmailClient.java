package com.tissue.api.notification.infrastructure.email;

import org.springframework.stereotype.Component;

import com.tissue.api.notification.domain.service.sender.EmailClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DummyEmailClient implements EmailClient {

	@Override
	public void send(String to, String subject, String body) {
		// 실제 전송하지 않고 로그로만 출력
		log.info("[DummyEmailClient] Email sent - receiver: {}, title: {}\nbody: {}", to, subject, body);
	}
}
