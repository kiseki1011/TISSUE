package com.uranus.taskmanager.api.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.uranus.taskmanager.api.domain.workspace.Workspace;
import com.uranus.taskmanager.api.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.response.WorkspaceResponse;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

	@InjectMocks
	private WorkspaceService workspaceService;

	@Mock
	private WorkspaceRepository workspaceRepository;

	@Test
	@DisplayName("Workspace를 생성하면 응답의 id는 1이어야 하고, 레포지토리는 한번 호출되어야 한다")
	public void test1() {
		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();
		Workspace mockWorkspace = Workspace.builder()
			.id(1L)
			.name("Test Workspace")
			.description("Test Description")
			.build();

		when(workspaceRepository.save(any(Workspace.class))).thenReturn(mockWorkspace);
		WorkspaceResponse response = workspaceService.create(request);

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo(1L);
		verify(workspaceRepository, times(1)).save(any(Workspace.class));

	}

}
