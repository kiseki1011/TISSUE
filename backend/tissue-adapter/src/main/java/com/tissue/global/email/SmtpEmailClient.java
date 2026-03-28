package com.tissue.global.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@RequiredArgsConstructor
public class SmtpEmailClient implements EmailClient {

    private final JavaMailSender mailSender;

    @Value("${tissue.email.sender}")
    private String senderEmail;

    @Override
    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = createMimeMessage(to, subject, body);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new MailSendException("Failed to send email to: " + to, e);
        }
    }

    private MimeMessage createMimeMessage(String to, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setFrom(senderEmail);
        helper.setText(body, true); // true = HTML content

        return message;
    }
}
