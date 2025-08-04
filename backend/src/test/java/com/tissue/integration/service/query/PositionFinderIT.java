package com.tissue.integration.service.query;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.position.presentation.dto.request.CreatePositionRequest;
import com.tissue.api.position.presentation.dto.response.GetPositionsResponse;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

class PositionFinderIT extends ServiceIntegrationTestHelper {

	Workspace workspace;

	@BeforeEach
	void setUp() {
		// create workspace
		workspace = testDataFixture.createWorkspace("test workspace", null, null);
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("워크스페이스가 가지고 있는 모든 Position의 목록을 조회할 수 있다")
	void canRetrieveAllPositionsWorkspaceHas() {
		// given
		CreatePositionRequest request1 = new CreatePositionRequest(
			"Backend Dev",
			"Backend Dev"
		);

		CreatePositionRequest request2 = new CreatePositionRequest(
			"Frontend Dev",
			"Frontend Dev"
		);

		CreatePositionRequest request3 = new CreatePositionRequest(
			"PM",
			"Project Manager"
		);

		// when
		positionCommandService.createPosition(workspace.getKey(), request1);
		positionCommandService.createPosition(workspace.getKey(), request2);
		positionCommandService.createPosition(workspace.getKey(), request3);

		GetPositionsResponse response = positionQueryService.getPositions(workspace.getKey());

		// then
		Assertions.assertThat(response.positions().get(0).name()).isEqualTo("Backend Dev");
		Assertions.assertThat(response.positions().get(1).name()).isEqualTo("Frontend Dev");
		Assertions.assertThat(response.positions().get(2).name()).isEqualTo("PM");
	}
}
