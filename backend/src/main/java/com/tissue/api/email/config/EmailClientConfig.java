package com.tissue.api.email.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import com.tissue.api.email.domain.EmailClient;
import com.tissue.api.email.infrastructure.DummyEmailClient;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class EmailClientConfig {

	private final JavaMailSender mailSender;

	// TODO: register different beans using @Profile
	@Bean
	public EmailClient emailClient() {
		return new DummyEmailClient();
		// return new GmailSmtpClient(mailSender);
	}
}
