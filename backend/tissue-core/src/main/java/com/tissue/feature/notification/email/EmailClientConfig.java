package com.tissue.feature.notification.email;

import com.tissue.feature.notification.application.port.email.EmailClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@RequiredArgsConstructor
public class EmailClientConfig {

    @Bean
    @ConditionalOnProperty(name = "tissue.email.client", havingValue = "dummy", matchIfMissing = true)
    public EmailClient dummyEmailClient() {
        return new DummyEmailClient();
    }

    @Bean
    @ConditionalOnProperty(name = "tissue.email.client", havingValue = "smtp")
    public EmailClient smtpEmailClient(JavaMailSender mailSender) {
        return new SmtpEmailClient(mailSender);
    }
}
