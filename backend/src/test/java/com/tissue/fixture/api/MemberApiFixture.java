package com.tissue.fixture.api;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.member.domain.JobType;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@Component
public class MemberApiFixture {

	public void signupApi(String loginId, String email, String password) {
		SignupMemberRequest signupMemberRequest = SignupMemberRequest.builder()
			.loginId(loginId)
			.email(email)
			.password(password)
			.firstName("Gildong")
			.lastName("Hong")
			.birthDate(LocalDate.of(1995, 1, 1))
			.introduction("Im a backend engineer.")
			.jobType(JobType.DEVELOPER)
			.build();

		RestAssured.given()
			.contentType(ContentType.JSON)
			.body(signupMemberRequest)
			.when()
			.post("/api/v1/members")
			.then()
			.statusCode(HttpStatus.CREATED.value())
			.extract().response();
	}
}
