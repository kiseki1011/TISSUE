package com.tissue.email.config;

import com.tissue.email.domain.EmailClient;
import com.tissue.email.infrastructure.DummyEmailClient;
import com.tissue.email.infrastructure.GmailSmtpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class EmailClientConfig {

    @Bean
    @ConditionalOnProperty(name = "tissue.email.provider", havingValue = "dummy", matchIfMissing = true)
    public EmailClient dummyEmailClient() {
        return new DummyEmailClient();
    }

    @Bean
    @ConditionalOnProperty(name = "tissue.email.provider", havingValue = "google")
    public EmailClient gmailEmailClient(JavaMailSender mailSender) {
        return new GmailSmtpClient(mailSender);
    }
}
