package com.uranus.taskmanager.api.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.uranus.taskmanager.api.domain.workspace.Workspace;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class CustomWorkspaceRepositoryImplTest {

	@Mock
	private EntityManager entityManager;

	@InjectMocks
	private CustomWorkspaceRepositoryImpl customWorkspaceRepository;

	@Test
	@DisplayName("saveWithNewTransaction은 persist를 통해 엔티티를 영속화 한다")
	void test() {
		// given
		Workspace workspace = Workspace.builder()
			.workspaceCode("abcd1234")
			.name("test name")
			.description("test description")
			.build();

		// when
		customWorkspaceRepository.saveWithNewTransaction(workspace);

		// then
		verify(entityManager, times(1)).persist(workspace);

	}

	@Test
	@DisplayName("saveWithNewTransaction 호출 시 저장된 Workspace의 속성이 정확히 반환되어야 한다")
	void test2() {
		// given
		Workspace workspace = Workspace.builder()
			.workspaceCode("abcd1234")
			.name("test name")
			.description("test description")
			.build();

		// when

		Workspace savedWorkspace = customWorkspaceRepository.saveWithNewTransaction(workspace);

		// then
		assertThat(savedWorkspace.getName()).isEqualTo("test name");
		assertThat(savedWorkspace.getDescription()).isEqualTo("test description");
		assertThat(savedWorkspace.getWorkspaceCode()).isEqualTo("abcd1234");

	}
}
