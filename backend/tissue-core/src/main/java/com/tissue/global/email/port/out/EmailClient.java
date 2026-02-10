package com.tissue.global.email.port.out;

public interface EmailClient {
    void send(String to, String subject, String body);
}
