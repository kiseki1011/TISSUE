package com.tissue.fixture.api;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.tissue.api.security.authentication.presentation.dto.request.LoginRequest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@Component
public class LoginApiFixture {

	public String loginWithIdApi(String loginId, String password) {
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

	public String loginWithEmailApi(String email, String password) {
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
