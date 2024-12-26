package com.tissue.integration.workspace;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.helper.RestAssuredTestHelper;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@Transactional
class WorkspaceApiIntegrationTest extends RestAssuredTestHelper {

	@BeforeEach
	public void setUp() {
		RestAssured.port = port;
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("워크스페이스를 생성하면 응답의 데이터에 워크스페이스의 이름, 설명, 코드가 존재해야 한다")
	void test1() {

		memberApiFixture.signupApi("user123", "user123@gmail.com", "test1234!");
		String sessionCookie = loginApiFixture.loginWithIdApi("user123", "test1234!");

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();

		RestAssured.given()
			.contentType(ContentType.JSON)
			.cookie("JSESSIONID", sessionCookie)
			.body(request)
			.when()
			.post("/api/v1/workspaces")
			.then()
			.statusCode(HttpStatus.CREATED.value())
			.body("data.id", equalTo(1))
			.extract().response();
	}

	@Test
	@DisplayName("하나의 워크스페이스를 생성하면 DB에 하나의 워크스페이스만 존재해야 한다")
	void test2() {

		memberApiFixture.signupApi("user123", "user123@gmail.com", "test1234!");
		String sessionCookie = loginApiFixture.loginWithIdApi("user123", "test1234!");

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();

		RestAssured.given()
			.contentType(ContentType.JSON)
			.cookie("JSESSIONID", sessionCookie)
			.body(request)
			.when()
			.post("/api/v1/workspaces")
			.then()
			.statusCode(HttpStatus.CREATED.value())
			.extract().response();

		assertThat(workspaceRepository.count()).isEqualTo(1L);
	}

	@Test
	@DisplayName("워크스페이스가 생성될 시 생성자(로그인ID)가 기록된다")
	void test3() {
		// given
		String loginId = "user123";
		String email = "user123@test.com";
		String password = "password123!";

		memberApiFixture.signupApi(loginId, email, password);
		String sessionCookie = loginApiFixture.loginWithIdApi(loginId, password);

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
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

		Workspace workspace = workspaceRepository.findById(1L).orElseThrow();
		Member member = memberRepository.findByLoginId("user123").orElseThrow();

		// then
		assertThat(workspace.getCreatedByMember()).isEqualTo(member.getId());
	}

	@Test
	@DisplayName("워크스페이스가 생성될 시 생성일이 기록된다")
	void test4() {
		// given
		String loginId = "user123";
		String email = "user123@test.com";
		String password = "password123!";

		memberApiFixture.signupApi(loginId, email, password);
		String sessionCookie = loginApiFixture.loginWithIdApi(loginId, password);

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
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
