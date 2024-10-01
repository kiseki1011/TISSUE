package com.uranus.taskmanager.api.workspace.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceResponse;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

	@InjectMocks
	private WorkspaceService workspaceService;

	@Mock
	private WorkspaceRepository workspaceRepository;

	@Test
	@DisplayName("유효한 workspaceCode로 워크스페이스를 조회하면, 워크스페이스를 반환한다.")
	void test2() {
		String workspaceCode = "testcode";
		Workspace mockWorkspace = Workspace.builder()
			.workspaceCode(workspaceCode)
			.name("Test Workspace")
			.description("Test Description")
			.build();

		when(workspaceRepository.findByWorkspaceCode(workspaceCode))
			.thenReturn(Optional.of(mockWorkspace));

		WorkspaceResponse response = workspaceService.get(workspaceCode);

		assertThat(response).isNotNull();
		assertThat(response.getWorkspaceCode()).isEqualTo(workspaceCode);
		verify(workspaceRepository, times(1)).findByWorkspaceCode(workspaceCode);
	}

	@Test
	@DisplayName("유효하지 workspaceCode로 워크스페이스를 조회하면, WorkspaceNotFoundException 발생")
	void test3() {
		String invalidWorkspaceId = UUID.randomUUID().toString();

		when(workspaceRepository.findByWorkspaceCode(invalidWorkspaceId))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> workspaceService.get(invalidWorkspaceId))
			.isInstanceOf(RuntimeException.class); // RuntimeException -> WorkspaceNotFoundException 변경 예정

		verify(workspaceRepository, times(1)).findByWorkspaceCode(invalidWorkspaceId);
	}

}
