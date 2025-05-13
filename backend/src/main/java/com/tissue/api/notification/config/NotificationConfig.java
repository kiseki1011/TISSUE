package com.tissue.api.notification.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.notification.domain.service.message.NotificationContentArgumentsFormatter;
import com.tissue.api.notification.domain.service.message.NotificationMessageFactory;
import com.tissue.api.notification.infrastructure.message.SimpleNotificationMessasgeFactory;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class NotificationConfig {

	private final MessageSource messageSource;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final NotificationContentArgumentsFormatter argumentFormatter;

	@Bean
	public NotificationMessageFactory notificationMessageFactory() {
		return new SimpleNotificationMessasgeFactory(messageSource, workspaceMemberReader, argumentFormatter);
	}
}
