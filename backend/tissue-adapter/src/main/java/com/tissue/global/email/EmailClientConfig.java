package com.tissue.global.email;

import com.tissue.support.email.EmailClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
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
        return new SmtpEmailClient(mailSender);
    }
}
