package com.tissue.global.email;

public interface EmailClient {
    void send(String to, String subject, String body);
}
