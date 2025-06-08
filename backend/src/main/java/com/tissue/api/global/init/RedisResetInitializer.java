package com.tissue.api.global.init;

import java.util.Objects;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

// TODO: application.yml 정리 필요
//  이런 코드 방식보다 test-container 또는 초기화 스크립트 고려
//  이 방식은 추후에 프로젝트가 커지면서 프로뎍션 코드에 섞여 들어갈 가능성 있음
//  만약 실수로 프로적션에서 실행하게 되면 대재앙!☠️
@Component
@Profile("ssr") // ssr 프로필에서만 작동
@RequiredArgsConstructor
public class RedisResetInitializer {

	private final RedisTemplate<String, ?> redisTemplate;

	@PostConstruct
	public void resetRedis() {
		Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().serverCommands();
	}
}
