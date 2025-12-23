package com.tissue.email.infrastructure;

import org.springframework.stereotype.Component;

import com.tissue.email.domain.EmailClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DummyEmailClient implements EmailClient {

	@Override
	public void send(String to, String subject, String body) {
		log.info("[DummyEmailClient] Email sent - receiver: {}, title: {}\nbody: {}", to, subject, body);
	}
}
