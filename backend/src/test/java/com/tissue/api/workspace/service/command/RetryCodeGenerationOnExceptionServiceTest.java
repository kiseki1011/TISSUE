package com.tissue.api.workspace.service.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.tissue.api.common.exception.type.InternalServerException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.util.WorkspaceCodeGenerator;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.helper.ServiceIntegrationTestHelper;

@SpringBootTest
class RetryCodeGenerationOnExceptionServiceTest extends ServiceIntegrationTestHelper {

	@MockBean
	private WorkspaceCodeGenerator workspaceCodeGenerator;

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	/**
	 * 단일 중복 확률: n/62^8 (n: 데이터베이스에 이미 존재하는 코드의 수)
	 * 재시도 5회 안에 중복을 해결하지 못할 확률: (n/62^8)^5
	 * n의 값이 10^9 이어도(데이터베이스에 이미 10억개의 코드가 저장된 경우) 중복을 해결 못할 가능성은 낮다
	 */
	@Test
	@DisplayName("최대 재시도 횟수(5)를 초과하면 예외가 발생한다")
	void whenCreatingWorkspace_MaxRetryLimitCannotBeExceeded() {
		// given
		Member member = testDataFixture.createMember("tester");

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("test workspace")
			.description("test workspace")
			.build();

		// assume the generated workspace code is always duplicate
		when(workspaceCodeGenerator.generateWorkspaceCode()).thenReturn("DUPLICATECODE");

		// create workspace that has the duplicate code as workspace code
		workspaceRepository.save(Workspace.builder()
			.name("existing workspace")
			.description("existing workspace")
			.code("DUPLICATECODE")
			.build());

		// when & then
		assertThatThrownBy(() -> workspaceCreateService.createWorkspace(request, member.getId()))
			.isInstanceOf(InternalServerException.class);

		verify(workspaceCodeGenerator, times(5)).generateWorkspaceCode();
	}
}
