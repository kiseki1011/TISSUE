package com.tissue.notification.adapter.out.sender;

import com.tissue.email.domain.EmailClient;
import com.tissue.notification.application.port.out.FailedEmailRepository;
import com.tissue.notification.domain.FailedEmail;
import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.service.NotificationSender;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailSender implements NotificationSender {

    private final EmailClient emailClient;
    private final MessageSource messageSource;
    private final FailedEmailRepository failedEmailRepository;

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

            subject = replacePlaceholders(titleTemplate, data);
            body = replacePlaceholders(contentTemplate, data);

            emailClient.send(to, subject, body);
        } catch (Exception e) {
            log.warn(
                    "failed to send email notification: receiver member id={}, cause={}",
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

    // TODO: Consider using Apache Commons Text - StringSubstitutor
    private String replacePlaceholders(String template, Map<String, String> data) {
        String result = template;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
