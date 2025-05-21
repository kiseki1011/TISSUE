package com.tissue.api.notification.domain.service.sender;

public interface EmailClient {
	void send(String to, String subject, String body);
}
