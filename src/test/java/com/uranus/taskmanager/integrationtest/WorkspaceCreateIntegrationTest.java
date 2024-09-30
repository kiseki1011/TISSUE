package com.uranus.taskmanager.integrationtest;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.basetest.BaseIntegrationTest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

class WorkspaceCreateIntegrationTest extends BaseIntegrationTest {

	@BeforeEach
	public void setup() {
		RestAssured.port = port;
		workspaceRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("워크스페이스를 생성하면 응답의 데이터에 워크스페이스의 이름, 설명, 코드가 존재해야 한다")
	public void test1() throws Exception {

		restAssuredAuthenticationFixture.signup("user123", "user123@gmail.com", "test1234!");
		String sessionCookie = restAssuredAuthenticationFixture.loginWithId("user123", "test1234!");

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();

		Response response = RestAssured.given()
			.contentType(ContentType.JSON)
			.cookie("JSESSIONID", sessionCookie)
			.body(request)
			.when()
			.post("/api/v1/workspaces")
			.then()
			.statusCode(HttpStatus.CREATED.value())
			.body("data.name", equalTo("Test Workspace"))
			.body("data.description", equalTo("Test Description"))
			.body("data.workspaceCode", notNullValue())
			.extract().response();

	}

	@Test
	@DisplayName("하나의 워크스페이스를 생성하면 DB에 하나의 워크스페이스만 존재해야 한다")
	public void test2() throws Exception {

		restAssuredAuthenticationFixture.signup("user123", "user123@gmail.com", "test1234!");
		String sessionCookie = restAssuredAuthenticationFixture.loginWithId("user123", "test1234!");

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();

		Response response = RestAssured.given()
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
}
