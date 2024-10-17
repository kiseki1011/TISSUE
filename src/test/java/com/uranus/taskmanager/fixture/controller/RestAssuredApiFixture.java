package com.uranus.taskmanager.fixture.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.uranus.taskmanager.api.authentication.dto.request.LoginRequest;
import com.uranus.taskmanager.api.member.dto.request.SignupRequest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@Component
public class RestAssuredApiFixture {

	public void signup(String loginId, String email, String password) {
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId(loginId)
			.email(email)
			.password(password)
			.build();

		Response response = RestAssured.given()
			.contentType(ContentType.JSON)
			.body(signupRequest)
			.when()
			.post("/api/v1/members/signup")
			.then()
			.statusCode(HttpStatus.CREATED.value())
			.extract().response();
	}

	public String loginWithId(String loginId, String password) {
		LoginRequest loginRequest = LoginRequest.builder()
			.loginId(loginId)
			.password(password)
			.build();

		Response response = RestAssured.given()
			.contentType(ContentType.JSON)
			.body(loginRequest)
			.when()
			.post("/api/v1/auth/login")
			.then()
			.statusCode(HttpStatus.OK.value())
			.extract().response();

		return response.cookie("JSESSIONID");
	}

	public String loginWithEmail(String email, String password) {
		LoginRequest loginRequest = LoginRequest.builder()
			.email(email)
			.password(password)
			.build();

		Response response = RestAssured.given()
			.contentType(ContentType.JSON)
			.body(loginRequest)
			.when()
			.post("/api/v1/auth/login")
			.then()
			.statusCode(HttpStatus.OK.value())
			.extract().response();

		return response.cookie("JSESSIONID");
	}
}
