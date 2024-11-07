package com.uranus.taskmanager.api.invitation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.uranus.taskmanager.api.authentication.LoginMemberArgumentResolver;
import com.uranus.taskmanager.api.authentication.SessionKey;
import com.uranus.taskmanager.api.authentication.dto.LoginMember;
import com.uranus.taskmanager.api.invitation.dto.response.InvitationAcceptResponse;
import com.uranus.taskmanager.api.invitation.exception.InvitationNotFoundException;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.fixture.entity.InvitationEntityFixture;
import com.uranus.taskmanager.fixture.entity.MemberEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceEntityFixture;
import com.uranus.taskmanager.helper.ControllerTestHelper;

/**
 * Todo: MockLoginArgumentResolver를 만들어서 테스트 코드 개선하기
 *  - 현재 컨트롤러 단위 테스트를 @MockBean을 사용해서 구현하고 있다
 *  - <br>
 *  - Q. @MockBean 사용의 문제점은 무엇일까?
 *  - A. @MockBean이나 @SpyBean의 문제점은 코드의 설계가 좋지 않음에도 불구하고 테스트가 너무 간편하다는 것이다
 *  - 예를 들면, 컨트롤러가 여러 서비스 객체에 강하게 의존하고 있다면, 이는 컨트롤러가 서비스 계층과 지나치게 엮여 있는 구조일 가능성이 높다
 *  - 그러나 @MockBean을 사용하면 이러한 복잡한 의존성을 신경 쓰지 않고도 테스트를 통과할 수 있게 된다
 *  - <br>
 *  - Q. 그럼 권장하는 테스트 작성법은 무엇인가?
 *  - 먼저 @MockBean 없이 테스트 코드를 작성한다(직접 필요한 서비스 모킹)
 *  - 만약 모킹할 대상이 너무 많아서 테스트 작성이 힘들면 프로덕션의 설계를 개선한다
 *  - 예시: 특정 컴포넌트를 별도의 클래스로 분리
 *  - 다시 테스트 작성
 *  - <br>
 *  - 참고
 *  - https://jojoldu.tistory.com/320
 *  - https://jojoldu.tistory.com/239
 *  - https://dadadamarine.github.io/java/spring/2019/04/26/spring-controller-test3.html
 *  - <br>
 */
class InvitationControllerTest extends ControllerTestHelper {

	@MockBean
	private LoginMemberArgumentResolver loginMemberArgumentResolver;

	WorkspaceEntityFixture workspaceEntityFixture;
	MemberEntityFixture memberEntityFixture;
	InvitationEntityFixture invitationEntityFixture;

	@BeforeEach
	public void setup() {
		workspaceEntityFixture = new WorkspaceEntityFixture();
		memberEntityFixture = new MemberEntityFixture();
		invitationEntityFixture = new InvitationEntityFixture();
	}

	@Test
	@DisplayName("초대를 수락이 성공하면 OK를 응답받고 초대 수락 응답 DTO를 데이터로 받는다")
	void test1() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		Workspace workspace = Workspace.builder()
			.code(workspaceCode)
			.name("workspace1")
			.description("description1")
			.build();

		InvitationAcceptResponse response = InvitationAcceptResponse.builder()
			.workspaceDetail(WorkspaceDetail.from(workspace, WorkspaceRole.USER))
			.nickname("member1@test.com")
			.build();

		LoginMember loginMember = LoginMember.builder()
			.id(1L)
			.loginId("member1")
			.email("member1@test.com")
			.build();

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, "1L");

		when(loginMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).thenReturn(loginMember);
		when(loginMemberArgumentResolver.supportsParameter(any())).thenReturn(true);
		when(invitationService.acceptInvitation(anyLong(), eq(workspaceCode))).thenReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/invitations/{workspaceCode}/accept", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Invitation Accepted"))
			.andExpect(jsonPath("$.data").exists())
			.andDo(print());
	}

	@Test
	@DisplayName("유효하지 않은 코드로 초대를 수락하면 예외가 발생한다")
	void test2() throws Exception {
		// given
		String workspaceCode = "INVALIDCODE";

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		LoginMember loginMember = LoginMember.builder()
			.id(1L)
			.loginId("member1")
			.email("member1@test.com")
			.build();

		when(loginMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).thenReturn(loginMember);
		when(loginMemberArgumentResolver.supportsParameter(any())).thenReturn(true);
		when(invitationService.acceptInvitation(1L, workspaceCode))
			.thenThrow(new InvitationNotFoundException());

		// when & then
		mockMvc.perform(post("/api/v1/invitations/{workspaceCode}/accept", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Invitation was not found for the given code"))
			.andDo(print());
	}

	@Test
	@DisplayName("초대를 거절하면 응답으로 OK를 받는다")
	void test3() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		LoginMember loginMember = LoginMember.builder()
			.id(1L)
			.loginId("member1")
			.email("member1@test.com")
			.build();

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, "1L");

		when(loginMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).thenReturn(loginMember);
		when(loginMemberArgumentResolver.supportsParameter(any())).thenReturn(true);

		// when & then
		mockMvc.perform(post("/api/v1/invitations/{workspaceCode}/reject", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Invitation Rejected"))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andDo(print());
	}

}
