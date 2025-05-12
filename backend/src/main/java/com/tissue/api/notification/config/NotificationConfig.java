package com.tissue.api.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.issue.application.service.reader.IssueReader;
import com.tissue.api.notification.domain.DefaultNotificationMessageFactory;
import com.tissue.api.notification.domain.NotificationMessageFactory;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class NotificationConfig {

	private final WorkspaceMemberReader workspaceMemberReader;
	private final IssueReader issueReader;

	@Bean
	public NotificationMessageFactory notificationMessageFactory() {
		return new DefaultNotificationMessageFactory(workspaceMemberReader, issueReader);
	}
}
