package com.tissue.feature.notification.scheduler;

import com.tissue.feature.notification.application.port.usecase.EmailRetryUseCase;
import com.tissue.feature.notification.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRetryScheduler implements SchedulingConfigurer {

    private final EmailRetryUseCase emailRetryUseCase;
    private final NotificationProperties notificationProperties;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedDelayTask(
                emailRetryUseCase::retryFailedEmails,
                notificationProperties.email().retryInterval());
    }
}
