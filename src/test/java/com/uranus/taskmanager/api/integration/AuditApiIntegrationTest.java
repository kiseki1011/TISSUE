package com.uranus.taskmanager.api.integration;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.fixture.api.LoginApiFixture;
import com.uranus.taskmanager.fixture.api.MemberApiFixture;
import com.uranus.taskmanager.helper.RestAssuredTestHelper;
import com.uranus.taskmanager.util.DatabaseCleaner;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class AuditApiIntegrationTest extends RestAssuredTestHelper {
	@Autowired
	private AuditorAware<String> auditorAware;
	@Autowired
	private LoginApiFixture loginApiFixture;
	@Autowired
	private MemberApiFixture memberApiFixture;
	@Autowired
	private DatabaseCleaner databaseCleaner;

	@BeforeEach
	public void setUp() {
		RestAssured.port = port;
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("워크스페이스가 생성될 시 생성자(로그인 정보의 로그인ID)가 기록된다")
	public void test1() {
		// given
		String loginId = "user123";
		String email = "user123@test.com";
		String password = "password123!";

		memberApiFixture.signupApi(loginId, email, password);
		String sessionCookie = loginApiFixture.loginWithIdApi(loginId, password);

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();

		// when
		RestAssured.given()
			.contentType(ContentType.JSON)
			.cookie("JSESSIONID", sessionCookie)
			.body(request)
			.when()
			.post("/api/v1/workspaces")
			.then()
			.statusCode(HttpStatus.CREATED.value())
			.extract().response();

		Optional<Workspace> optionalWorkspace = workspaceRepository.findById(1L);
		// then
		assertThat(optionalWorkspace).isPresent();
		Workspace workspace = optionalWorkspace.get();
		assertThat(workspace.getCreatedBy()).isEqualTo(loginId);
	}

	@Test
	@DisplayName("워크스페이스가 생성될 시 생성일이 기록된다")
	public void test2() {
		// given
		String loginId = "user123";
		String email = "user123@test.com";
		String password = "password123!";

		memberApiFixture.signupApi(loginId, email, password);
		String sessionCookie = loginApiFixture.loginWithIdApi(loginId, password);

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();

		// when
		RestAssured.given()
			.contentType(ContentType.JSON)
			.cookie("JSESSIONID", sessionCookie)
			.body(request)
			.when()
			.post("/api/v1/workspaces")
			.then()
			.statusCode(HttpStatus.CREATED.value())
			.extract().response();

		Optional<Workspace> optionalWorkspace = workspaceRepository.findById(1L);
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime oneSecondAgo = now.minusSeconds(1);

		// then
		assertThat(optionalWorkspace).isPresent();
		assertThat(optionalWorkspace.get().getCreatedDate()).isBefore(now);
		assertThat(optionalWorkspace.get().getCreatedDate()).isAfter(oneSecondAgo);
	}
}
