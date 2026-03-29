package com.tissue.feature.notification.application.port.email;

public interface EmailClient {
    void send(String to, String subject, String body);
}
