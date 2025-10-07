package deprecated.com.tissue.unit.controller;

import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Locale;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.member.domain.model.enums.JobType;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberEmailRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberProfileRequest;
import com.tissue.api.member.presentation.dto.request.WithdrawMemberRequest;
import com.tissue.api.member.presentation.dto.response.command.MemberResponse;
import com.tissue.api.member.presentation.dto.response.query.GetProfileResponse;

import deprecated.com.tissue.support.helper.ControllerTestHelper;

class MemberControllerTest extends ControllerTestHelper {

	@Test
	@DisplayName("GET /members - 멤버 프로필(상세 정보) 조회에 성공하면 OK")
	void getMyProfile_success_OK() throws Exception {
		// given
		// MockHttpSession session = new MockHttpSession();
		// session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		// when & then
		mockMvc.perform(get("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Found profile."))
			.andDo(print());
	}

	@Test
	@DisplayName("GET /members - 멤버 프로필 조회에 성공하면 응답 데이터는 멤버의 상세 정보이다")
	void getMyProfile_success_returnMemberDetail() throws Exception {
		// given
		Member member = Member.builder()
			.loginId("tester")
			.email("test@test.com")
			.name("Gildong Hong")
			.birthDate(LocalDate.of(1990, 1, 1))
			.jobType(JobType.DEVELOPER)
			.build();

		when(memberQueryService.getProfile(anyLong())).thenReturn(GetProfileResponse.from(member));

		// when & then
		mockMvc.perform(get("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Found profile."))
			.andExpect(jsonPath("$.data.name").value("Gildong Hong"))
			.andExpect(jsonPath("$.data.birthDate").value(LocalDate.of(1990, 1, 1).toString()))
			.andDo(print());
	}

	@Test
	@DisplayName("POST /members - 회원 가입에 성공하면 CREATED")
	void signup_success_CREATED() throws Exception {
		// given
		SignupMemberRequest request = SignupMemberRequest.builder()
			.loginId("testuser")
			.email("test@test.com")
			.username("testusername")
			.password("test1234!")
			.name("Gildong Hong")
			.birthDate(LocalDate.of(1995, 1, 1))
			.jobType(JobType.DEVELOPER)
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andDo(print());
	}

	@ParameterizedTest
	@CsvSource({
		"test!!",
		"한글아이디",
		"test한글",
	})
	@DisplayName("POST /members - 회원 가입에 로그인 아이디는 영문 소문자로 시작하고 영문 소문자와 숫자만 포함해야 한다")
	void signup_loginIdPattern_mustBeLowercaseWithNumbers(String loginId) throws Exception {
		// given
		SignupMemberRequest request = SignupMemberRequest.builder()
			.loginId(loginId)
			.email("test@test.com")
			.username("testusername")
			.password("test1234!")
			.name("Gildong Hong")
			.birthDate(LocalDate.of(1995, 1, 1))
			.jobType(JobType.DEVELOPER)
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Accept-Language", "en")
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data..message")
				.value(messageSource.getMessage("valid.pattern.id", null, Locale.ENGLISH)))
			.andDo(print());
	}

	@ParameterizedTest
	@CsvSource({
		"Test1234",
		"패스워드검증실패",
		"Test1234!한글"
	})
	@DisplayName("POST /members - 회원 가입 시 패스워드는 하나 이상의 영문자, 숫자와 특수문자를 포함한 조합이어야 한다")
	void signup_passwordPattern_mustBeAlphaNumericWithSpecialCharacter(String password) throws Exception {

		SignupMemberRequest signupMemberRequest = SignupMemberRequest.builder()
			.loginId("testuser1234")
			.email("testemail@gmail.com")
			.username("testusername")
			.password(password)
			.name("Gildong Hong")
			.birthDate(LocalDate.of(1995, 1, 1))
			.jobType(JobType.DEVELOPER)
			.build();

		mockMvc.perform(post("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(signupMemberRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data..message")
				.value(messageSource.getMessage("valid.pattern.password", null, Locale.ENGLISH)))
			.andDo(print());
	}

	static Stream<Arguments> provideInvalidInputs() {
		return Stream.of(
			arguments(null, null, null), // null
			arguments("", "", ""), // 빈 문자열
			arguments(" ", " ", " ") // 공백
		);
	}

	@ParameterizedTest
	@MethodSource("provideInvalidInputs")
	@DisplayName("POST /members - 회원 가입에 로그인 아이디, 이메일, 패스워드는 null, 공백, 빈 문자이면 안된다")
	void signUp_loginId_email_password_mustNotBeBlank(String loginId, String email, String password) throws Exception {
		// given
		SignupMemberRequest signupMemberRequest = SignupMemberRequest.builder()
			.loginId(loginId)
			.email(email)
			.password(password)
			.username("testusername")
			.name("Gildong Hong")
			.birthDate(LocalDate.of(1995, 1, 1))
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
	@DisplayName("PATCH /members - 멤버 상세 정보(프로필) 업데이트에 성공하면 OK")
	void updateMemberInfo_success_OK() throws Exception {
		// given
		UpdateMemberProfileRequest request = UpdateMemberProfileRequest.builder()
			.birthDate(LocalDate.of(1995, 1, 1))
			.jobType(JobType.DEVELOPER)
			.build();

		Long memberId = 1L;

		MemberResponse response = new MemberResponse(memberId);

		when(memberCommandService.updateInfo(any(UpdateMemberProfileRequest.class), anyLong()))
			.thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member info updated."))
			.andExpect(jsonPath("$.data.memberId").value(memberId))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /members - 멤버 프로필 업데이트 시 생일을 현재 날짜 이후로 설정하면 검증에 실패한다")
	void updateMemberInfo_fail_ifBirthDateIsLaterThanNow() throws Exception {
		// given
		UpdateMemberProfileRequest request = UpdateMemberProfileRequest.builder()
			.birthDate(LocalDate.of(2995, 1, 1))
			.build();

		Long memberId = 1L;

		when(memberCommandService.updateInfo(any(UpdateMemberProfileRequest.class), anyLong()))
			.thenReturn(new MemberResponse(memberId));

		// when & then
		mockMvc.perform(patch("/api/v1/members")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("One or more fields have failed validation."))
			.andExpect(
				jsonPath("$.data..message").value(messageSource.getMessage("valid.birthdate", null, Locale.ENGLISH)))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /members/email - 이메일 업데이트를 성공하면 OK")
	void updateEmail_success_OK() throws Exception {
		// given
		UpdateMemberEmailRequest request = new UpdateMemberEmailRequest("newemail@test.com");

		// when & then
		mockMvc.perform(patch("/api/v1/members/email")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member email updated."))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /members/email - 이메일 업데이트를 성공하면 응답 데이터에 해당 Member의 id가 포함된다")
	void updateEmail_success_responseDataHasEmail() throws Exception {
		// given
		UpdateMemberEmailRequest request = new UpdateMemberEmailRequest("newemail@test.com");
		Long memberId = 1L;

		MemberResponse response = new MemberResponse(memberId);

		when(memberCommandService.updateEmail(any(UpdateMemberEmailRequest.class), anyLong()))
			.thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/members/email")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member email updated."))
			.andExpect(jsonPath("$.data.memberId").value(memberId))
			.andDo(print());
	}

	@Test
	@DisplayName("DELETE /members - 멤버의 회원 탈퇴에 성공하면 OK")
	void withdrawMember_success_OK() throws Exception {
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
