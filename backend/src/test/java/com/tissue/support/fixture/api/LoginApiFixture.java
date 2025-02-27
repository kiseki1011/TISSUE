package com.tissue.support.fixture.api;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.tissue.api.security.authentication.presentation.dto.request.LoginRequest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@Component
public class LoginApiFixture {

	public String loginWithIdApi(String identifier, String password) {
		LoginRequest loginRequest = LoginRequest.builder()
			.identifier(identifier)
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

	public String loginWithEmailApi(String identifier, String password) {
		LoginRequest loginRequest = LoginRequest.builder()
			.identifier(identifier)
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
