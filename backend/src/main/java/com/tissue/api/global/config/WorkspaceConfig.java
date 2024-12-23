package com.tissue.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.workspace.service.command.create.RetryCodeGenerationOnExceptionService;
import com.tissue.api.workspace.service.command.create.WorkspaceCreateService;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.util.WorkspaceCodeGenerator;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;

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
