package com.tissue.email.domain;

public interface EmailClient {
	void send(String to, String subject, String body);
}
