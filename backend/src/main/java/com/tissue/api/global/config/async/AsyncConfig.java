package com.tissue.api.global.config.async;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

	/**
	 * Todo
	 *  - 필요한 경우 Epic의 StoryPoint 업데이트를 위한 이벤트 핸들러에 @Async를 적용하자(비동기로 구현)
	 */
	@Bean(name = "epicTaskExecutor")
	public Executor epicTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		executor.setCorePoolSize(2); // 기본 스레드 풀 크기
		executor.setMaxPoolSize(5); // 최대 스레드 풀 크기
		executor.setQueueCapacity(50); // 작업 큐 용량 - 모든 스레드가 사용 중일 때 대기할 수 있는 작업 수
		executor.setThreadNamePrefix("EpicEvent-");

		// 작업을 처리하지 못한 경우의 정책
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();

		return executor;
	}
}
