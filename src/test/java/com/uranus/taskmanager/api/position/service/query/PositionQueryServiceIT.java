package com.uranus.taskmanager.api.position.service.query;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uranus.taskmanager.api.position.presentation.dto.request.CreatePositionRequest;
import com.uranus.taskmanager.api.position.presentation.dto.response.GetPositionsResponse;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class PositionQueryServiceIT extends ServiceIntegrationTestHelper {

	@BeforeEach
	void setUp() {
		// 테스트용 워크스페이스 생성
		workspaceRepositoryFixture.createAndSaveWorkspace(
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
	@DisplayName("워크스페이스가 가지고 있는 모든 Position의 목록을 조회할 수 있다")
	void test() {
		// given
		CreatePositionRequest request1 = new CreatePositionRequest(
			"Backend Dev",
			"Backend Developer"
		);

		CreatePositionRequest request2 = new CreatePositionRequest(
			"Frontend Dev",
			"Frontend Developer"
		);

		CreatePositionRequest request3 = new CreatePositionRequest(
			"PM",
			"Project Manager"
		);

		// when
		positionCommandService.createPosition("TESTCODE", request1);
		positionCommandService.createPosition("TESTCODE", request2);
		positionCommandService.createPosition("TESTCODE", request3);

		GetPositionsResponse response = positionQueryService.getPositions("TESTCODE");

		// then
		Assertions.assertThat(response.positions().get(0).name()).isEqualTo("Backend Dev");
		Assertions.assertThat(response.positions().get(1).name()).isEqualTo("Frontend Dev");
		Assertions.assertThat(response.positions().get(2).name()).isEqualTo("PM");

	}
}
