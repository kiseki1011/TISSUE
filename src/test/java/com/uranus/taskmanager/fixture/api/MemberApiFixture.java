package com.uranus.taskmanager.fixture.api;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.uranus.taskmanager.api.member.presentation.dto.request.SignupMemberRequest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@Component
public class MemberApiFixture {

	public void signupApi(String loginId, String email, String password) {
		SignupMemberRequest signupMemberRequest = SignupMemberRequest.builder()
			.loginId(loginId)
			.email(email)
			.password(password)
			.build();

		Response response = RestAssured.given()
			.contentType(ContentType.JSON)
			.body(signupMemberRequest)
			.when()
			.post("/api/v1/members/signup")
			.then()
			.statusCode(HttpStatus.CREATED.value())
			.extract().response();
	}
}
