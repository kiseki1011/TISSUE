package com.tissue.api.workspace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.member.service.query.MemberQueryService;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.util.RandomNicknameGenerator;
import com.tissue.api.util.WorkspaceCodeGenerator;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.service.command.create.RetryCodeGenerationOnExceptionService;
import com.tissue.api.workspace.service.command.create.WorkspaceCreateService;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Todo: @Qualifier 또는 @Primary의 사용을 고려한다
 */
@Configuration
@RequiredArgsConstructor
public class WorkspaceConfig {

	private final MemberQueryService memberQueryService;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceCodeGenerator workspaceCodeGenerator;
	private final RandomNicknameGenerator randomNicknameGenerator;
	private final PasswordEncoder passwordEncoder;

	/**
	 * RetryCodeGenerationOnExceptionService: DB에서 올라오는 DataIntegrityViolationException을 잡아서 핸들링(워크스페이스 코드 재생성)
	 * CheckCodeDuplicationService: 서비스 계층에서 워크스페이스 코드의 중복을 미리 검사
	 */
	@Bean
	public WorkspaceCreateService workspaceCreateService() {
		return new RetryCodeGenerationOnExceptionService(
			memberQueryService,
			workspaceRepository,
			workspaceMemberRepository,
			workspaceCodeGenerator,
			randomNicknameGenerator,
			passwordEncoder
		);
	}
}
