package com.tissue.integration.service.query;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.position.presentation.dto.request.CreatePositionRequest;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

public class PositionQueryServiceIT extends ServiceIntegrationTestHelper {

	Workspace workspace;

	@BeforeEach
	void setUp() {
		workspace = testDataFixture.createWorkspace("test workspace", null, null);
	}

	// TODO: 포지션 생성 시, 색을 정할 수 있도록 설정
	@Test
	@DisplayName("특정 워크스페이스의 모든 Position이 사용한 모든 색(ColorType)을 조회할 수 있다")
	void canCreatePosition() {
		// Given
		positionCommandService.createPosition(
			workspace.getCode(),
			new CreatePositionRequest(
				"Backend Developer",
				null
			)
		);

		positionCommandService.createPosition(
			workspace.getCode(),
			new CreatePositionRequest(
				"Frontend Developer",
				null
			)
		);

		// When
		Set<ColorType> usedColors = positionQueryService.getUsedColors(workspace.getCode());

		// Then
		// TODO: usedColors에 생성한 포지션들의 색이 포함되는지 검증
	}
}
