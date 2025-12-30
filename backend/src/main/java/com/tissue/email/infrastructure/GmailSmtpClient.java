package com.tissue.email.infrastructure;

import com.tissue.email.domain.EmailClient;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;

@RequiredArgsConstructor
public class GmailSmtpClient implements EmailClient {

    private final JavaMailSender mailSender;

    @Value("${tissue.email.sender}")
    private String senderEmail;

    /**
     * Sends an email asynchronously.
     *
     * <p>Refactored to be non-blocking using @Async. This prevents the main transaction from
     * hanging if the SMTP server is slow.
     */
    @Async
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
        helper.setText(body, false); // false = plain text

        return message;
    }
}
