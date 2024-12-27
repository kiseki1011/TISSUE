package com.tissue.api.workspace.service.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.util.WorkspaceCodeGenerator;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.WorkspaceCodeCollisionHandleException;
import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.service.command.create.RetryCodeGenerationOnExceptionService;
import com.tissue.fixture.repository.MemberRepositoryFixture;
import com.tissue.util.DatabaseCleaner;

import jakarta.persistence.EntityManager;

@SpringBootTest
class RetryCodeGenerationOnExceptionServiceTest {

	@Autowired
	private RetryCodeGenerationOnExceptionService workspaceCreateService;

	@Autowired
	private WorkspaceRepository workspaceRepository;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private DatabaseCleaner databaseCleaner;
	@Autowired
	private MemberRepositoryFixture memberRepositoryFixture;

	@MockBean
	private WorkspaceCodeGenerator workspaceCodeGenerator;

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("최대 재시도 횟수를 초과하면 예외가 발생한다")
	void createWorkspace_ExceedMaxRetries() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"tester",
			"test@test.com",
			"password1234!"
		);

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();

		// 항상 중복되는 코드 반환
		when(workspaceCodeGenerator.generateWorkspaceCode())
			.thenReturn("DUPLICATE-CODE");

		// 중복 코드를 가진 워크스페이스 미리 생성
		workspaceRepository.save(Workspace.builder()
			.name("Existing Workspace")
			.description("Existing Description")
			.code("DUPLICATE-CODE")
			.build());

		// when & then
		assertThatThrownBy(() ->
			workspaceCreateService.createWorkspace(request, member.getId())
		)
			.isInstanceOf(WorkspaceCodeCollisionHandleException.class);

		verify(workspaceCodeGenerator, times(5)).generateWorkspaceCode();
	}
}
