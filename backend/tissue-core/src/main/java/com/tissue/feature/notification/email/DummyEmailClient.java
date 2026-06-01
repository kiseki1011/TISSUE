package com.tissue.feature.notification.email;

import com.tissue.feature.notification.application.port.email.EmailClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DummyEmailClient implements EmailClient {

    @Override
    public void send(String to, String subject, String body) {
        log.info("Email sent - receiver: {}, subject: {}\nbody: {}", to, subject, body);
    }
}
