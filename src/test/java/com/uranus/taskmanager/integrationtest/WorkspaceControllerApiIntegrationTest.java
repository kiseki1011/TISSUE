package com.uranus.taskmanager.integrationtest;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.basetest.BaseApiIntegrationTest;
import com.uranus.taskmanager.fixture.controller.RestAssuredApiFixture;
import com.uranus.taskmanager.util.DatabaseCleaner;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@Transactional
class WorkspaceControllerApiIntegrationTest extends BaseApiIntegrationTest {

	/**
	 * Todo: Workspace와 WorkspaceMember 엔티티 간의 외래 키 제약 조건으로 인한 문제 발생
	 * Workspace 엔티티를 삭제하려고 할 때, 해당 Workspace와 연결된 WorkspaceMember 엔티티 레코드가 존재하기 때문에
	 * 참조 무결성 위반 문제가 발생한다
	 * 쉽게 말해서, WorkspaceMember 테이블에서 Workspace에 대한 외래 키 제약 조건이 설정되어 있어서,
	 * 해당 Workspace를 먼저 삭제하려면 해당 외래 키로 참조하는 모든 WorkspaceMember 레코드를 먼저 삭제해야 한다
	 * <p>
	 * 해결 1: workspaceMemberRepository.deleteAll()을 제일 먼저 수행한다
	 * 해결 2: CascadeType.REMOVE을 사용한다
	 * - 예시: Workspace 엔티티에서 연관관계 매핑과 관련해서 cascade = CascadeType.ALL, orphanRemoval = true을 사용한다
	 */
	@Autowired
	private RestAssuredApiFixture restAssuredApiFixture;
	@Autowired
	private DatabaseCleaner databaseCleaner;

	@BeforeEach
	public void setup() {
		RestAssured.port = port;
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("워크스페이스를 생성하면 응답의 데이터에 워크스페이스의 이름, 설명, 코드가 존재해야 한다")
	public void test1() throws Exception {

		restAssuredApiFixture.signup("user123", "user123@gmail.com", "test1234!");
		String sessionCookie = restAssuredApiFixture.loginWithId("user123", "test1234!");

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
			.body("data.code", notNullValue())
			.extract().response();
	}

	@Test
	@DisplayName("하나의 워크스페이스를 생성하면 DB에 하나의 워크스페이스만 존재해야 한다")
	public void test2() throws Exception {

		restAssuredApiFixture.signup("user123", "user123@gmail.com", "test1234!");
		String sessionCookie = restAssuredApiFixture.loginWithId("user123", "test1234!");

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
