package com.tissue.api.workspace.validator;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.workspace.exception.InvalidWorkspacePasswordException;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;
import com.tissue.helper.ServiceIntegrationTestHelper;

class WorkspaceValidatorTest extends ServiceIntegrationTestHelper {

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("검증하는 워크스페이스 코드를 이미 사용중이면 false를 반환한다")
	void validateWorkspaceCodeIsUnique_returnsFalse_ifCodeIsNotUnique() {
		// given
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Workspace",
			"TESTCODE",
			null
		);

		// when
		boolean result = workspaceValidator.validateWorkspaceCodeIsUnique("TESTCODE");

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("검증하는 패스워드가 워크스페이스의 암호화된 패스워드와 일치하지 않으면 예외가 발생한다")
	void ifPassword_doesNotMatch_workspacePassword_throwException() {
		// given
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Workspace",
			"TESTCODE",
			"test1234!"
		);

		// when & then
		assertThatThrownBy(() -> workspaceValidator.validateWorkspacePassword("invalidPassword", "TESTCODE"))
			.isInstanceOf(InvalidWorkspacePasswordException.class);
	}

	@Test
	@DisplayName("워크스페이스의 패스워드와 맞는지 검증할 때, 코드에 대한 워크스페이스가 존재하지 않으면 예외가 발생한다")
	void ifWorkspaceCode_workspaceDoesNotExist() {
		// given
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Workspace",
			"TESTCODE",
			"test1234!"
		);

		// when & then
		assertThatThrownBy(() -> workspaceValidator.validateWorkspacePassword("test1234!", "NOTFOUND"))
			.isInstanceOf(WorkspaceNotFoundException.class);
	}
}