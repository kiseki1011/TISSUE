package com.tissue.feature.notification.email;

import com.tissue.feature.notification.application.port.email.EmailClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@RequiredArgsConstructor
public class EmailClientConfig {

    @Bean
    @Profile("test")
    public EmailClient dummyEmailClient() {
        return new DummyEmailClient();
    }

    @Bean
    @Profile("!test")
    public EmailClient smtpEmailClient(JavaMailSender mailSender) {
        return new SmtpEmailClient(mailSender);
    }
}
