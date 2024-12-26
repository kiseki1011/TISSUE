package com.tissue.api.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.tissue.api.security.authorization.exception.InvalidWorkspaceCodeInUriException;

class WorkspaceCodeParserTest {
	private WorkspaceCodeParser workspaceCodeParser;

	@BeforeEach
	void setUp() {
		workspaceCodeParser = new WorkspaceCodeParser();
	}

	@ParameterizedTest
	@CsvSource({
		"/api/v1/workspaces/WORKSPC1, WORKSPC1",
		"/api/v1/workspaces/WORKSPC1/some/path, WORKSPC1",
		"/api/v1/workspaces/ANOTHER1/, ANOTHER1"
	})
	@DisplayName("유효한 URI에서 워크스페이스 코드를 추출할 수 있다")
	void extractValidWorkspaceCode(String uri, String expectedCode) {
		assertThat(workspaceCodeParser.extractWorkspaceCode(uri)).isEqualTo(expectedCode);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"/api/v1/workspaces//",
		"/api/v1/workspaces/",
		"/api/v1/workspaces/ABC",  // 8자리가 아닌 경우
		"/api/v1/workspaces/ABC@1234"  // 허용되지 않는 문자 포함
	})
	@DisplayName("잘못된 형식의 URI에서 코드 추출을 시도하면 예외가 발생한다")
	void throwExceptionForInvalidUri(String uri) {
		assertThatThrownBy(() -> workspaceCodeParser.extractWorkspaceCode(uri))
			.isInstanceOf(InvalidWorkspaceCodeInUriException.class);
	}
}