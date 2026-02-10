package com.tissue.global.email;

import com.tissue.global.email.port.out.EmailClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DummyEmailClient implements EmailClient {

    @Override
    public void send(String to, String subject, String body) {
        log.info("[DummyEmailClient] Email sent - receiver: {}, title: {}\nbody: {}", to, subject, body);
    }
}
