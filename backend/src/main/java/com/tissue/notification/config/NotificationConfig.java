package com.tissue.notification.config;

import com.tissue.notification.domain.service.message.NotificationContentArgumentsFormatter;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;

@Configuration
// @ComponentScan(basePackageClasses = NotificationSender.class)
@RequiredArgsConstructor
public class NotificationConfig {

    private final MessageSource messageSource;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final NotificationContentArgumentsFormatter argumentFormatter;
}
