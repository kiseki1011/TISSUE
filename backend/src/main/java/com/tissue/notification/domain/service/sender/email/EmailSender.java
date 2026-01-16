package com.tissue.notification.domain.service.sender.email;

import com.tissue.email.domain.EmailClient;
import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.service.sender.NotificationSender;
import java.util.List;
import java.util.Locale;
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
            List<String> args = notification.getMessage().args();
            Object[] argArray = args.toArray();

            String titleKey = "event." + type.name() + ".title";
            String contentKey = "event." + type.name() + ".content";

            String subject = messageSource.getMessage(titleKey, argArray, titleKey, locale);
            String body = messageSource.getMessage(contentKey, argArray, contentKey, locale);

            emailClient.send(to, subject, body);
        } catch (Exception e) {
            log.warn(
                    "failed to send email notification: receiver member id={}, cause={}",
                    notification.getReceiverMemberId(),
                    e.getMessage(),
                    e);
        }
    }
}
