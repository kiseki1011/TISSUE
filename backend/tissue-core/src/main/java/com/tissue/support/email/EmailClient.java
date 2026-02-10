package com.tissue.support.email;

public interface EmailClient {
    void send(String to, String subject, String body);
}
