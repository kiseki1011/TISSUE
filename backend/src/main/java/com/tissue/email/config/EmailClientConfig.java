package com.tissue.email.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.tissue.email.domain.EmailClient;
import com.tissue.email.infrastructure.GmailSmtpClient;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class EmailClientConfig {

	private final JavaMailSender mailSender;

	@Bean
	@ConditionalOnProperty(name = "tissue.email.provider", havingValue = "dummy", matchIfMissing = true)
	public EmailClient dummyEmailClient() {
		return new DummyEmailClient();
	}

	@Bean
	@ConditionalOnProperty(name = "tissue.email.provider", havingValue = "google")
	public EmailClient gmailEmailClient() {
		return new GmailSmtpClient(mailSender);
	}
}
