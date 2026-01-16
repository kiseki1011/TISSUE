package com.tissue.notification.domain.service.sender;

import com.tissue.email.domain.EmailClient;
import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;
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

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        try {
            String to = notification.getReceiverEmail();

            Locale locale = notification.getReceiverLanguage().getLocale();
            NotificationType type = notification.getType();
            Map<String, String> data = notification.getMessage().data();

            String titleKey = "event." + type.name() + ".title";
            String contentKey = "event." + type.name() + ".content";

            String titleTemplate = messageSource.getMessage(titleKey, null, titleKey, locale);
            String contentTemplate = messageSource.getMessage(contentKey, null, contentKey, locale);

            String subject = replacePlaceholders(titleTemplate, data);
            String body = replacePlaceholders(contentTemplate, data);

            emailClient.send(to, subject, body);
        } catch (Exception e) {
            log.warn(
                    "failed to send email notification: receiver member id={}, cause={}",
                    notification.getReceiverMemberId(),
                    e.getMessage(),
                    e);
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
