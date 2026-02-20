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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final EmailClient emailClient;
    private final MessageSource messageSource;
    private final FailedEmailRepository failedEmailRepository;
    private final NotificationTemplateRenderer templateRenderer;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        String subject = "";
        String body = "";
        try {
            String to = notification.getReceiverEmail();

            Locale locale = notification.getReceiverLanguage().getLocale();
            NotificationType type = notification.getType();
            Map<String, String> data = notification.getMessage().data();

            String titleKey = "event." + type.name() + ".title";
            String contentKey = "event." + type.name() + ".content";

            String titleTemplate = messageSource.getMessage(titleKey, null, titleKey, locale);
            String contentTemplate = messageSource.getMessage(contentKey, null, contentKey, locale);

            String content = templateRenderer.renderString(contentTemplate, data);

            // TODO: Add actionUrl/actionText to data if available
            body = templateRenderer.renderHtml(
                    "mail/notification-email",
                    Map.of("title", templateRenderer.renderString(titleTemplate, data), "content", content));

            subject = templateRenderer.renderString(titleTemplate, data);

            emailClient.send(to, subject, body);
        } catch (Exception e) {
            log.warn(
                    "Failed to send email notification: receiver member id={}, cause={}",
                    notification.getReceiverMemberId(),
                    e.getMessage(),
                    e);

            try {
                failedEmailRepository.save(FailedEmail.builder()
                        .notificationId(notification.getId())
                        .receiverEmail(notification.getReceiverEmail())
                        .subject(subject)
                        .body(body)
                        .errorMessage(e.getMessage())
                        .build());
            } catch (Exception dbEx) {
                log.error("Failed to save FailedEmail entity", dbEx);
            }
        }
    }
}
