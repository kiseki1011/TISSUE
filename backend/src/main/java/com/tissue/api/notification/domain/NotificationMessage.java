package com.tissue.api.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record NotificationMessage(
	@Column(nullable = false)
	String title,

	@Column(length = 1000)
	String content
) {
	public NotificationMessage() {
		this("", "");
	}
}
