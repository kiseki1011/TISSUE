package com.uranus.taskmanager.api.workspace.service.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.util.WorkspaceCodeGenerator;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceCodeCollisionHandleException;
import com.uranus.taskmanager.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.uranus.taskmanager.api.workspace.service.command.create.RetryCodeGenerationOnExceptionService;
import com.uranus.taskmanager.fixture.repository.MemberRepositoryFixture;
import com.uranus.taskmanager.util.DatabaseCleaner;

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
	@Disabled("saveWithNewTransaction을 사용하면 테스트 가능 - 테스트 환경에서 트랜잭션과 세션의 동작방식에 의한 문제 발생")
	@DisplayName("워크스페이스 코드 충돌이 발생하면 최대 5번까지 재시도한다")
	void createWorkspace_RetryOnCodeCollision() {
		// given
		Member member = memberRepository.save(Member.builder()
			.email("test@test.com")
			.password("password1234!")
			.loginId("tester")
			.build());

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();

		workspaceRepository.save(Workspace.builder()
			.name("Existing Workspace")
			.description("Existing Description")
			.code("DUPLICATE-CODE")
			.build()
		);

		// 처음 4번은 중복되는 코드 반환, 5번째에 성공
		when(workspaceCodeGenerator.generateWorkspaceCode())
			.thenReturn("DUPLICATE-CODE")  // 1차 시도
			.thenReturn("DUPLICATE-CODE")  // 2차 시도
			.thenReturn("DUPLICATE-CODE")  // 3차 시도
			.thenReturn("DUPLICATE-CODE")  // 4차 시도
			.thenReturn("SUCCESS-CODE");   // 5차 시도

		// when
		CreateWorkspaceResponse response = workspaceCreateService.createWorkspace(request, member.getId());

		// then
		assertThat(response.code()).isEqualTo("SUCCESS-CODE");
		verify(workspaceCodeGenerator, times(5)).generateWorkspaceCode();
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
