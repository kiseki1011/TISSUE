package deprecated.com.tissue.integration.service.command;

import deprecated.com.tissue.support.helper.ServiceIntegrationTestHelper;

class AuthenticationServiceIT extends ServiceIntegrationTestHelper {

	// @BeforeEach
	// void setup() {
	// 	databaseCleaner.execute();
	// }
	//
	// @Test
	// @DisplayName("가입된 멤버의 로그인ID로 로그인이 가능하다")
	// void canLoginWithLoginId() {
	// 	// given
	// 	Member member = testDataFixture.createMember("tester"); // password: test1234!
	//
	// 	LoginRequest loginRequest = LoginRequest.builder()
	// 		.identifier(member.getLoginId())
	// 		.password("test1234!")
	// 		.build();
	//
	// 	// when
	// 	LoginResponse loginResponse = authenticationService.login(loginRequest);
	//
	// 	// then
	// 	assertThat(loginResponse.accessToken()).isNotNull();
	// 	assertThat(loginResponse.refreshToken()).isNotNull();
	// }
	//
	// @Test
	// @DisplayName("가입된 멤버의 이메일로 로그인할 수 있다")
	// void canLoginWithEmail() {
	// 	// given
	// 	Member member = testDataFixture.createMember("tester"); // password: test1234!
	//
	// 	LoginRequest loginRequest = LoginRequest.builder()
	// 		.identifier(member.getEmail())
	// 		.password("test1234!")
	// 		.build();
	//
	// 	// when
	// 	LoginResponse loginResponse = authenticationService.login(loginRequest);
	//
	// 	// then
	// 	assertThat(loginResponse.accessToken()).isNotNull();
	// 	assertThat(loginResponse.refreshToken()).isNotNull();
	// }
	//
	// @Test
	// @DisplayName("유효하지 않은 로그인ID 또는 이메일로 로그인할 수 없다")
	// void cannotLoginWithInvalidLoginIdOrEmail() {
	// 	// given
	// 	Member member = testDataFixture.createMember("tester");
	//
	// 	LoginRequest loginRequest = LoginRequest.builder()
	// 		.identifier("nottester")
	// 		.password("test1234!")
	// 		.build();
	//
	// 	// when & then
	// 	assertThatThrownBy(() -> authenticationService.login(loginRequest))
	// 		.isInstanceOf(BadCredentialsException.class);
	// }
	//
	// @Test
	// @DisplayName("유효하지 않은 패스워드로 로그인할 수 없다")
	// void cannotLoginWithInvalidPassword() {
	// 	// given
	// 	Member member = testDataFixture.createMember("tester"); // password: test1234!
	//
	// 	LoginRequest loginRequest = LoginRequest.builder()
	// 		.identifier("tester")
	// 		.password("wrongpassword123!")
	// 		.build();
	//
	// 	// when & then
	// 	assertThatThrownBy(() -> authenticationService.login(loginRequest))
	// 		.isInstanceOf(BadCredentialsException.class);
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("Refresh 토큰이 유효한 경우, 새로운 Access 토큰을 생성할 수 있다")
	// void canCreateNewAccessToken() {
	// 	// given
	// 	Member member = testDataFixture.createTestMember(1L, "testuser");
	// 	entityManager.flush();
	// 	entityManager.clear();
	//
	// 	String refreshToken = jwtTokenService.createRefreshToken(1L, "testuser");
	//
	// 	// when
	// 	RefreshTokenResponse response = authenticationService.refreshToken(new RefreshTokenRequest(refreshToken));
	//
	// 	// then
	// 	assertThat(response.accessToken()).isNotNull();
	// 	jwtTokenService.validateAccessToken(response.accessToken());
	// }
}
