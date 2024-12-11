package com.uranus.taskmanager.api.position.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uranus.taskmanager.api.common.ColorPalette;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.position.domain.Position;
import com.uranus.taskmanager.api.position.exception.DuplicatePositionNameException;
import com.uranus.taskmanager.api.position.exception.PositionInUseException;
import com.uranus.taskmanager.api.position.presentation.dto.request.CreatePositionRequest;
import com.uranus.taskmanager.api.position.presentation.dto.request.UpdatePositionRequest;
import com.uranus.taskmanager.api.position.presentation.dto.response.CreatePositionResponse;
import com.uranus.taskmanager.api.position.presentation.dto.response.UpdatePositionResponse;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class PositionCommandServiceIT extends ServiceIntegrationTestHelper {

	private Workspace workspace;

	@BeforeEach
	void setUp() {
		// 테스트용 워크스페이스 생성
		workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("Position을 생성하면 생성 응답 반환")
	void createPosition() {
		// Given
		CreatePositionRequest request = new CreatePositionRequest(
			"Developer",
			"Backend Developer"
		);

		// When
		CreatePositionResponse createResponse = positionCommandService.createPosition("TESTCODE", request);

		// Then
		assertThat(createResponse.name()).isEqualTo("Developer");
	}

	@Test
	@DisplayName("Position을 생성하면 ColorPalette의 색상 중 랜덤한 색을 배정받는다")
	void createPosition_assignedRandomColor() {
		// Given
		CreatePositionRequest request = new CreatePositionRequest(
			"Developer",
			"Backend Developer"
		);

		// When
		CreatePositionResponse createResponse = positionCommandService.createPosition("TESTCODE", request);

		// Then
		Position findPosition = positionRepository.findById(createResponse.id()).orElseThrow();

		assertThat(findPosition.getColor()).isNotNull();
		assertThat(findPosition.getColor()).isInstanceOf(ColorPalette.class);
	}

	@Test
	@DisplayName("이름이 중복되는 Position을 생성하면 예외 발생")
	void createDuplicatePosition_throwsException() {
		// given
		CreatePositionRequest request1 = new CreatePositionRequest(
			"Developer",
			"Backend Developer"
		);

		positionCommandService.createPosition("TESTCODE", request1);

		CreatePositionRequest request2 = new CreatePositionRequest(
			"Developer",
			"Backend Developer2"
		);

		// when & then
		assertThatThrownBy(() -> positionCommandService.createPosition("TESTCODE", request2))
			.isInstanceOf(DuplicatePositionNameException.class);
	}

	@Test
	@DisplayName("Position을 수정하면 수정 응답 반환")
	void updatePosition() {
		// Given
		Position position = workspace.createPosition(
			"Developer",
			"Backend Developer",
			ColorPalette.BLACK
		);
		positionRepository.save(position);

		UpdatePositionRequest request = new UpdatePositionRequest(
			"Senior Developer",
			"Senior Backend Developer"
		);

		// When
		UpdatePositionResponse response = positionCommandService.updatePosition(
			"TESTCODE",
			position.getId(),
			request
		);

		// Then
		assertThat(response.name()).isEqualTo("Senior Developer");
		assertThat(response.description()).isEqualTo("Senior Backend Developer");
	}

	@Test
	@DisplayName("사용 중인 Position 삭제를 시도하면 예외 발생")
	void deletePosition_WhenInUse_ThrowsException() {
		// Given
		Position position = workspace.createPosition(
			"Developer",
			"Backend Developer",
			ColorPalette.BLACK
		);
		positionRepository.save(position);

		// WorkspaceMember 생성 및 Position 할당
		Member member = memberRepositoryFixture.createAndSaveMember(
			"testuser",
			"test@test.com",
			"test1234!"
		);

		WorkspaceMember workspaceMember = WorkspaceMember.addWorkspaceMember(
			member,
			workspace,
			WorkspaceRole.COLLABORATOR,
			"nickname"
		);
		workspaceMember.changePosition(position);
		workspaceMemberRepository.save(workspaceMember);

		// When & Then
		assertThatThrownBy(() ->
			positionCommandService.deletePosition("TESTCODE", position.getId())
		).isInstanceOf(PositionInUseException.class);
	}

	@Test
	@DisplayName("존재하지 않는 워크스페이스의 Position 생성 시 예외 발생")
	void createPosition_WithNonExistentWorkspace_ThrowsException() {
		// Given
		String nonExistentCode = "INVALID";
		CreatePositionRequest request = new CreatePositionRequest("Developer", "Backend Developer");

		// When & Then
		assertThatThrownBy(() ->
			positionCommandService.createPosition(nonExistentCode, request)
		).isInstanceOf(WorkspaceNotFoundException.class);
	}
}
