package com.uranus.taskmanager.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uranus.taskmanager.api.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.service.CheckCodeDuplicationService;
import com.uranus.taskmanager.api.service.WorkspaceCreateService;
import com.uranus.taskmanager.api.util.WorkspaceCodeGenerator;

import lombok.RequiredArgsConstructor;

/**
 * Todo: @Qualifier 또는 @Primary의 사용을 고려한다
 */
@Configuration
@RequiredArgsConstructor
public class WorkspaceConfig {

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceCodeGenerator workspaceCodeGenerator;

	/**
	 * HandleDatabaseExceptionService: DB에서 올라오는 ConstraintViolation을 잡아서 핸들링(워크스페이스 코드 재생성)
	 * CheckCodeDuplicationService: 서비스 계층에서 워크스페이스 코드의 중복을 미리 검사
	 */
	@Bean
	public WorkspaceCreateService workspaceCreateService() {
		return new CheckCodeDuplicationService(workspaceRepository, workspaceCodeGenerator);
	}
}
