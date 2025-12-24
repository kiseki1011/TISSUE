package com.tissue.email.infrastructure;

import org.springframework.scheduling.annotation.Async;

import com.tissue.email.domain.EmailClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DummyEmailClient implements EmailClient {

	@Async
	@Override
	public void send(String to, String subject, String body) {
		log.info("[DummyEmailClient] Email sent - receiver: {}, title: {}\nbody: {}", to, subject, body);
	}
}
