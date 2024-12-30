package com.tissue.api.member.presentation.controller;

import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.tissue.api.member.domain.JobType;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.vo.Name;
import com.tissue.api.member.exception.InvalidMemberPasswordException;
import com.tissue.api.member.presentation.dto.request.PermissionRequest;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberEmailRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberInfoRequest;
import com.tissue.api.member.presentation.dto.request.WithdrawMemberRequest;
import com.tissue.api.member.presentation.dto.response.MyProfileResponse;
import com.tissue.api.member.presentation.dto.response.UpdateMemberEmailResponse;
import com.tissue.api.member.presentation.dto.response.UpdateMemberInfoResponse;
import com.tissue.api.security.authentication.exception.InvalidLoginPasswordException;
import com.tissue.api.security.session.SessionAttributes;
import com.tissue.helper.ControllerTestHelper;

class MemberControllerTest extends ControllerTestHelper {

	static Stream<Arguments> provideInvalidInputs() {
		return Stream.of(
			arguments(null, null, null), // null
			arguments("", "", ""), // 빈 문자열
			arguments(" ", " ", " ") // 공백
		);
	}

	@Test
	@DisplayName("GET /members - 멤버 프로필(상세 정보) 조회에 성공하면 OK를 기대한다")
	void getMyProfile_success_OK() throws Exception {
		// given
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		// when & then
		mockMvc.perform(get("/api/v1/members")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Found my profile."))
			.andDo(print());
	}

	@Test
	@DisplayName("GET /members - 멤버 프로필 조회에 성공하면 멤버의 상세 정보를 응답 데이터로 받는다")
	void getMyProfile_success_responeDataIsMemberDetail() throws Exception {
		// given
		Member member = Member.builder()
			.loginId("tester")
			.email("test@test.com")
			.name(Name.builder()
				.firstName("Gildong")
				.lastName("Hong")
				.build())
			.birthDate(LocalDate.of(1990, 1, 1))
			.jobType(JobType.DEVELOPER)
			.biography("Im a backend developer.")
			.build();

		when(memberQueryService.getMyProfile(anyLong())).thenReturn(MyProfileResponse.from(member));

		// when & then
		mockMvc.perform(get("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Found my profile."))
			.andExpect(jsonPath("$.data.firstName").value("Gildong"))
			.andExpect(jsonPath("$.data.birthDate").value(LocalDate.of(1990, 1, 1).toString()))
			.andDo(print());
	}

	@Test
	@DisplayName("POST /members - 회원 가입에 검증을 통과하면 CREATED를 기대한다")
	void test1() throws Exception {
		SignupMemberRequest signupMemberRequest = SignupMemberRequest.builder()
			.loginId("testuser1234")
			.email("testemail@gmail.com")
			.password("Testpassword1234!")
			.firstName("Gildong")
			.lastName("Hong")
			.birthDate(LocalDate.of(1995, 1, 1))
			.biography("Im a backend engineer.")
			.jobType(JobType.DEVELOPER)
			.build();
		String requestBody = objectMapper.writeValueAsString(signupMemberRequest);

		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andDo(print());
	}

	@ParameterizedTest
	@CsvSource({
		"'testtesttesttesttest1', 'Login ID must be alphanumeric and must be between 2 and 20 characters'",
		"'1', 'Login ID must be alphanumeric and must be between 2 and 20 characters'",
		"'test!!', 'Login ID must be alphanumeric and must be between 2 and 20 characters'",
		"'한글아이디', 'Login ID must be alphanumeric and must be between 2 and 20 characters'",
		"'test1한글', 'Login ID must be alphanumeric and must be between 2 and 20 characters'",
	})
	@DisplayName("POST /members - 회원 가입에 loginId는 영문과 숫자 조합에 2~20자를 지켜야한다")
	void test2(String loginId, String loginIdValidMsg) throws Exception {
		// given
		SignupMemberRequest request = SignupMemberRequest.builder()
			.loginId(loginId)
			.email("testemail@gmail.com")
			.password("Testpassword1234!")
			.firstName("Gildong")
			.lastName("Hong")
			.birthDate(LocalDate.of(1995, 1, 1))
			.biography("Im a backend engineer.")
			.jobType(JobType.DEVELOPER)
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data..message").value(loginIdValidMsg))
			.andDo(print());
	}

	@ParameterizedTest
	@CsvSource({
		"'test', 'The password must be alphanumeric "
			+ "including at least one special character and must be between 8 and 30 characters'",
		"'Test1234', 'The password must be alphanumeric "
			+ "including at least one special character and must be between 8 and 30 characters'",
		"'한글패스워드', 'The password must be alphanumeric "
			+ "including at least one special character and must be between 8 and 30 characters'",
		"'Test1234!한글', 'The password must be alphanumeric "
			+ "including at least one special character and must be between 8 and 30 characters'",
	})
	@DisplayName("POST /members - 회원 가입에 password는 하나 이상의 영문자, 숫자와 특수문자를 포함한 조합에 8~30자를 지켜야한다")
	void test3(String password, String passwordValidMsg) throws Exception {

		SignupMemberRequest signupMemberRequest = SignupMemberRequest.builder()
			.loginId("testuser1234")
			.email("testemail@gmail.com")
			.password(password)
			.firstName("Gildong")
			.lastName("Hong")
			.birthDate(LocalDate.of(1995, 1, 1))
			.biography("Im a backend engineer.")
			.jobType(JobType.DEVELOPER)
			.build();

		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(signupMemberRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data..message").value(passwordValidMsg))
			.andDo(print());
	}

	@ParameterizedTest
	@MethodSource("provideInvalidInputs")
	@DisplayName("POST /members - 회원 가입에 loginId, email, password는 null, 공백, 빈 문자이면 안된다")
	void test4(String loginId, String email, String password) throws Exception {
		// given
		SignupMemberRequest signupMemberRequest = SignupMemberRequest.builder()
			.loginId(loginId)
			.email(email)
			.password(password)
			.firstName("Gildong")
			.lastName("Hong")
			.birthDate(LocalDate.of(1995, 1, 1))
			.biography("Im a backend engineer.")
			.jobType(JobType.DEVELOPER)
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(signupMemberRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data[*].field").value(Matchers.hasItem("loginId")))
			.andExpect(jsonPath("$.data[*].field").value(Matchers.hasItem("email")))
			.andExpect(jsonPath("$.data[*].field").value(Matchers.hasItem("password")))
			.andDo(print());
	}

	@Test
	@DisplayName("POST /members/permissions/update - 업데이트 권한 요청에 성공하면 OK를 응답한다")
	void getUpdateAuthorization_success_OK() throws Exception {
		// given
		PermissionRequest request = new PermissionRequest("password1234!");

		doNothing()
			.when(memberValidator)
			.validateMemberPassword(anyString(), anyLong());

		// when & then
		mockMvc.perform(post("/api/v1/members/permissions/update")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Update permission granted."))
			.andDo(print());
	}

	@Test
	@DisplayName("POST /members/permissions/update - 패스워드 검증에 실패하면 업데이트 권한 요청에 대해 UNAUTHORIZED를 응답한다")
	void getUpdateAuthorization_returnsUnauthorized_whenInvalidMemberPasswordException() throws Exception {
		// given
		PermissionRequest request = new PermissionRequest("password1234!");

		doThrow(new InvalidMemberPasswordException())
			.when(memberValidator)
			.validateMemberPassword(anyString(), anyLong());

		// when & then
		mockMvc.perform(post("/api/v1/members/permissions/update")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("The given password is invalid"))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /members - 멤버 상세 정보(프로필) 업데이트에 성공하면 OK를 응답한다")
	void updateMemberInfo_success_OK() throws Exception {
		// given
		UpdateMemberInfoRequest request = UpdateMemberInfoRequest.builder()
			.birthDate(LocalDate.of(1995, 1, 1))
			.jobType(JobType.DEVELOPER)
			.biography("Im a backend developer")
			.build();

		Member member = Member.builder()
			.loginId("tester")
			.email("test@test.com")
			.name(Name.builder()
				.firstName("Gildong")
				.lastName("Hong")
				.build())
			.birthDate(LocalDate.of(1995, 1, 1))
			.jobType(JobType.DEVELOPER)
			.biography("Im a backend developer")
			.build();

		when(memberCommandService.updateInfo(any(UpdateMemberInfoRequest.class), anyLong()))
			.thenReturn(UpdateMemberInfoResponse.from(member));

		// when & then
		mockMvc.perform(patch("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member info updated."))
			.andExpect(jsonPath("$.data.memberId").value(member.getId()))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /members - 멤버 상세 정보(프로필) 업데이트 시 생일을 현재 날짜 이후로 설정하면 검증에 실패한다")
	void updateMemberInfo_fail_ifBirthDateIsLaterThanNow() throws Exception {
		// given
		UpdateMemberInfoRequest request = UpdateMemberInfoRequest.builder()
			.birthDate(LocalDate.of(2995, 1, 1))
			.build();

		Member member = Member.builder()
			.loginId("tester")
			.email("test@test.com")
			.name(Name.builder()
				.firstName("Gildong")
				.lastName("Hong")
				.build())
			.birthDate(LocalDate.of(1995, 1, 1))
			.jobType(JobType.DEVELOPER)
			.biography("Im a backend developer")
			.build();

		when(memberCommandService.updateInfo(any(UpdateMemberInfoRequest.class), anyLong()))
			.thenReturn(UpdateMemberInfoResponse.from(member));

		// when & then
		mockMvc.perform(patch("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("One or more fields have validation errors"))
			.andExpect(jsonPath("$.data..message").value("Birth date must be in the past"))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /members/email - 이메일 업데이트를 성공하면 OK를 응답한다")
	void updateEmail_success_OK() throws Exception {
		// given
		UpdateMemberEmailRequest request = new UpdateMemberEmailRequest("password1234!", "newemail@test.com");

		// when & then
		mockMvc.perform(patch("/api/v1/members/email")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member email updated."))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /members/email - 이메일 업데이트를 성공하면 이메일 업데이트 응답을 데이터로 받는다")
	void updateEmail_success_returnsMemberEmailUpdateResponse() throws Exception {
		// given
		UpdateMemberEmailRequest request = new UpdateMemberEmailRequest("password1234!", "newemail@test.com");

		UpdateMemberEmailResponse response = UpdateMemberEmailResponse.from(
			Member.builder()
				.email("newemail@test.com")
				.build());

		when(memberCommandService.updateEmail(any(UpdateMemberEmailRequest.class), anyLong())).thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/members/email")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member email updated."))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /members/email - 이메일 업데이트 요청 시 패스워드 검증을 실패하면 UNAUTHORIZED을 응답한다")
	void updateEmail_forbidden_ifUpdateAuthIsInvalid() throws Exception {
		// given
		UpdateMemberEmailRequest request = new UpdateMemberEmailRequest("password1234!", "newemail@test.com");

		doThrow(new InvalidLoginPasswordException())
			.when(memberValidator)
			.validateMemberPassword(eq("password1234!"), anyLong());

		// when & then
		mockMvc.perform(patch("/api/v1/members/email")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value(
				"The given login password is invalid"))
			.andDo(print());
	}

	@Test
	@DisplayName("DELETE /members - 멤버의 회원 탈퇴에 성공하면 OK를 응답받는다")
	void withdrawMember_shouldReturn200_ifSuccess() throws Exception {
		// given
		WithdrawMemberRequest request = new WithdrawMemberRequest("password1234!");

		// when & then
		mockMvc.perform(delete("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member withdrawal successful."))
			.andDo(print());
	}
}
