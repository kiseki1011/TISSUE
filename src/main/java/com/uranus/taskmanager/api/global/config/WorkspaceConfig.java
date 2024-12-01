package com.uranus.taskmanager.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.util.WorkspaceCodeGenerator;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.service.command.create.RetryCodeGenerationOnExceptionService;
import com.uranus.taskmanager.api.workspace.service.command.create.WorkspaceCreateService;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Todo: @Qualifier 또는 @Primary의 사용을 고려한다
 */
@Configuration
@RequiredArgsConstructor
public class WorkspaceConfig {

	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceCodeGenerator workspaceCodeGenerator;
	private final PasswordEncoder passwordEncoder;

	/**
	 * RetryCodeGenerationOnExceptionService: DB에서 올라오는 DataIntegrityViolationException을 잡아서 핸들링(워크스페이스 코드 재생성)
	 * CheckCodeDuplicationService: 서비스 계층에서 워크스페이스 코드의 중복을 미리 검사
	 */
	@Bean
	public WorkspaceCreateService workspaceCreateService() {
		return new RetryCodeGenerationOnExceptionService(workspaceRepository,
			memberRepository,
			workspaceMemberRepository,
			workspaceCodeGenerator,
			passwordEncoder);
	}
}
