package com.tissue.feature.notification.sender;

import com.tissue.feature.notification.application.port.repository.FailedEmailRepository;
import com.tissue.feature.notification.application.port.repository.NotificationTemplateRenderer;
import com.tissue.feature.notification.domain.FailedEmail;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.enums.NotificationChannel;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.notification.domain.service.NotificationSender;
import com.tissue.support.email.EmailClient;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

    private final EmailClient emailClient;
    private final MessageSource messageSource;
    private final FailedEmailRepository failedEmailRepository;
    private final NotificationTemplateRenderer templateRenderer;
    private final Executor emailExecutor;

    public EmailNotificationSender(
            EmailClient emailClient,
            MessageSource messageSource,
            FailedEmailRepository failedEmailRepository,
            NotificationTemplateRenderer templateRenderer,
            @Qualifier("emailExecutor") Executor emailExecutor) {
        this.emailClient = emailClient;
        this.messageSource = messageSource;
        this.failedEmailRepository = failedEmailRepository;
        this.templateRenderer = templateRenderer;
        this.emailExecutor = emailExecutor;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public Executor getExecutor() {
        return emailExecutor;
    }

    @Override
    public void send(Notification notification) {
        String to = notification.getReceiverEmail();
        if (to == null) {
            return;
        }

        String subject;
        String body;
        try {
            Locale locale = notification.getReceiverLanguage().getLocale();
            NotificationType type = notification.getNotificationType();
            Map<String, String> data = notification.getMessage().data();

            String titleKey = "event." + type.name() + ".title";
            String contentKey = "event." + type.name() + ".content";

            String titleTemplate = messageSource.getMessage(titleKey, null, titleKey, locale);
            String contentTemplate = messageSource.getMessage(contentKey, null, contentKey, locale);

            String content = templateRenderer.renderString(contentTemplate, data);

            // TODO: Add actionUrl/actionText to data if available
            subject = templateRenderer.renderString(titleTemplate, data);
            body = templateRenderer.renderHtml("mail/notification-email", Map.of("title", subject, "content", content));

        } catch (Exception e) {
            log.error(
                    "Failed to render email template: receiver member id={}, cause={}",
                    notification.getReceiverMemberId(),
                    e.getMessage(),
                    e);
            return;
        }

        try {
            emailClient.send(to, subject, body);
        } catch (Exception e) {
            log.warn(
                    "Failed to send email notification: receiver member id={}, cause={}",
                    notification.getReceiverMemberId(),
                    e.getMessage(),
                    e);
            saveFailedEmail(notification, to, subject, body, e);
        }
    }

    private void saveFailedEmail(
            Notification notification, String receiverEmail, String subject, String body, Exception cause) {
        try {
            failedEmailRepository.save(FailedEmail.builder()
                    .notificationId(notification.getId())
                    .receiverEmail(receiverEmail)
                    .subject(subject)
                    .body(body)
                    .errorMessage(cause.getMessage())
                    .build());

        } catch (Exception e) {
            log.error("Failed to save FailedEmail entity for notification id={}", notification.getId(), e);
        }
    }
}
