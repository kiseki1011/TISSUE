package com.tissue.email.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import com.tissue.email.domain.EmailClient;
import com.tissue.email.infrastructure.GmailSmtpClient;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class EmailClientConfig {

	private final JavaMailSender mailSender;

	@Bean
	public EmailClient emailClient() {
		// return new DummyEmailClient();
		return new GmailSmtpClient(mailSender);
	}
}
