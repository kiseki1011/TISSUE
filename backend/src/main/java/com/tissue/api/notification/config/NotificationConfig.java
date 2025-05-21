package com.tissue.api.notification.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.notification.domain.service.message.NotificationContentArgumentsFormatter;
import com.tissue.api.notification.domain.service.message.NotificationMessageFactory;
import com.tissue.api.notification.infrastructure.message.SimpleNotificationMessageFactory;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Configuration
// @ComponentScan(basePackageClasses = NotificationSender.class)
@RequiredArgsConstructor
public class NotificationConfig {

	private final MessageSource messageSource;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final NotificationContentArgumentsFormatter argumentFormatter;

	@Bean
	public NotificationMessageFactory notificationMessageFactory() {
		return new SimpleNotificationMessageFactory(messageSource, workspaceMemberReader, argumentFormatter);
	}

	// TODO(고민중): 사용할 EmailClient 구현체 선택? notification 도메인은 별도의 모듈로 분리 예정?
}
