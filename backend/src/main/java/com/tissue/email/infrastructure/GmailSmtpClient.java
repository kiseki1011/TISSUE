package com.tissue.email.infrastructure;

import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.tissue.email.domain.EmailClient;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GmailSmtpClient implements EmailClient {

	private final JavaMailSender mailSender;

	// TODO: needs refactor
	@Override
	public void send(String to, String subject, String body) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

			helper.setTo(to);
			helper.setSubject(subject);
			helper.setFrom("your-email@gmail.com");
			helper.setText(body, false); // false = plain text

			mailSender.send(message);
		} catch (MessagingException e) {
			throw new MailSendException("Failed to send email to: " + to, e);
		}
	}
}
