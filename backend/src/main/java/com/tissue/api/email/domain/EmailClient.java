package com.tissue.api.email.domain;

public interface EmailClient {
	void send(String to, String subject, String body);
}
