package com.tissue.feature.notification.email;

import com.tissue.feature.notification.application.port.email.EmailClient;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@RequiredArgsConstructor
public class SmtpEmailClient implements EmailClient {

    private static final String LOGO_CONTENT_ID = "tissueLogo";
    private static final Resource LOGO_RESOURCE = new ClassPathResource("mail/tissue-logo.png");

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
        boolean withInlineLogo = body.contains("cid:" + LOGO_CONTENT_ID);
        MimeMessageHelper helper = new MimeMessageHelper(message, withInlineLogo, "utf-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setFrom(senderEmail);
        helper.setText(body, true); // true = HTML content

        if (withInlineLogo) {
            helper.addInline(LOGO_CONTENT_ID, LOGO_RESOURCE, "image/png");
        }

        return message;
    }
}
