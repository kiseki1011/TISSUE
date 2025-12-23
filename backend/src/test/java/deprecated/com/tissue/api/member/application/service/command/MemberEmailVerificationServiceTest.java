package deprecated.com.tissue.api.member.application.service.command;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberEmailVerificationServiceTest {
	// @Mock
	// private EmailVerificationRepository repository;
	//
	// @Mock
	// private EmailClient emailClient;
	//
	// @Mock
	// private EmailVerificationProperties properties;
	//
	// @InjectMocks
	// private MemberEmailVerificationService service;
	//
	// // TODO: baseurl properties로 사용
	// @Test
	// @DisplayName("이메일 인증 요청을 보내면 토큰이 저장되고 이메일이 전송된다")
	// void sendVerificationEmail_savesTokenAndSendsEmail() {
	// 	// given
	// 	String email = "test@example.com";
	// 	String expectedBaseUrl = "http://localhost:8080/api/v1/members/email-verification/verify";
	//
	// 	when(properties.getVerificationUrl()).thenReturn(expectedBaseUrl);
	//
	// 	// when
	// 	service.sendVerificationEmail(email);
	//
	// 	// then
	// 	// 토큰 저장 호출이 있었는지 검증
	// 	verify(repository).saveToken(eq(email), anyString(), eq(Duration.ofMinutes(30)));
	//
	// 	// 메일 전송이 되었는지 검증
	// 	verify(emailClient).send(eq(email), anyString(), contains(expectedBaseUrl));
	// }
	//
	// @Test
	// @DisplayName("토큰 검증 요청을 위임한다")
	// void verifyEmail_delegatesToRepository() {
	// 	// given
	// 	String email = "test@example.com";
	// 	String token = "sometoken";
	// 	when(repository.verify(email, token)).thenReturn(true);
	//
	// 	// when
	// 	boolean result = service.verifyEmail(email, token);
	//
	// 	// then
	// 	assertThat(result).isTrue();
	// }
	//
	// @Test
	// @DisplayName("이메일이 인증되지 않은 경우 예외를 던진다")
	// void validateEmailVerified_throwsExceptionIfNotVerified() {
	// 	// given
	// 	String email = "test@example.com";
	// 	when(repository.isVerified(email)).thenReturn(false);
	//
	// 	// expect
	// 	assertThatThrownBy(() -> service.validateEmailVerified(email))
	// 		// TODO: EmailNotVerifiedException
	// 		.isInstanceOf(RuntimeException.class)
	// 		.hasMessageContaining("not verified");
	// }
	//
	// @Test
	// @DisplayName("이메일이 인증되었는지 확인한다")
	// void isEmailVerified_returnsTrueIfVerified() {
	// 	// given
	// 	String email = "test@example.com";
	// 	when(repository.isVerified(email)).thenReturn(true);
	//
	// 	// when
	// 	boolean result = service.isEmailVerified(email);
	//
	// 	// then
	// 	assertThat(result).isTrue();
	// }
}