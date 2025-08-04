package com.tissue.api.workspace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tissue.api.member.application.service.command.MemberFinder;
import com.tissue.api.workspace.application.service.command.create.WorkspaceCreateRetryOnCodeCollisionService;
import com.tissue.api.workspace.application.service.command.create.WorkspaceCreateService;
import com.tissue.api.workspace.infrastructure.repository.WorkspaceRepository;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WorkspaceConfig {

	private final MemberFinder memberFinder;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * RetryCodeGenerationOnExceptionService: DB에서 올라오는 DataIntegrityViolationException을 잡아서 핸들링(워크스페이스 코드 재생성)
	 * CheckCodeDuplicationService: 서비스 계층에서 워크스페이스 코드의 중복을 미리 검사
	 */
	@Bean
	public WorkspaceCreateService workspaceCreateService() {
		return new WorkspaceCreateRetryOnCodeCollisionService(
			memberFinder,
			workspaceRepository,
			workspaceMemberRepository,
			passwordEncoder
		);
	}
}
