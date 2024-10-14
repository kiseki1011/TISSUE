package com.uranus.taskmanager.api.invitation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.uranus.taskmanager.api.auth.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.invitation.InvitationStatus;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.dto.response.InvitationAcceptResponse;
import com.uranus.taskmanager.api.invitation.exception.InvalidInvitationStatusException;
import com.uranus.taskmanager.api.invitation.exception.InvitationNotFoundException;
import com.uranus.taskmanager.api.invitation.repository.InvitationRepository;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.fixture.TestFixture;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

	@InjectMocks
	private InvitationService invitationService;

	@Mock
	private WorkspaceRepository workspaceRepository;
	@Mock
	private MemberRepository memberRepository;
	@Mock
	private WorkspaceMemberRepository workspaceMemberRepository;
	@Mock
	private InvitationRepository invitationRepository;

	TestFixture testFixture;

	@BeforeEach
	public void setup() {
		testFixture = new TestFixture();
	}

	@Test
	@DisplayName("유효한 로그인 정보와 코드를 사용해서 초대를 수락하면 초대가 저장된다")
	void test1() {
		// given
		String workspaceCode = "testcode";
		String loginId = "user123";
		String email = "user123@test.com";

		Workspace workspace = testFixture.createWorkspace(workspaceCode);
		Member member = testFixture.createMember(loginId, email);
		WorkspaceMember workspaceMember = testFixture.createUserWorkspaceMember(member, workspace);
		LoginMemberDto loginMember = testFixture.createLoginMemberDto(loginId, email);
		Invitation invitation = testFixture.createPendingInvitation(workspace, member);

		when(invitationRepository.findByWorkspaceCodeAndMemberLoginId(workspaceCode, loginId)).thenReturn(
			Optional.of(invitation));
		when(invitationRepository.save(eq(invitation))).thenReturn(invitation);
		when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(workspaceMember);

		// when
		InvitationAcceptResponse acceptResponse = invitationService.acceptInvitation(loginMember, workspaceCode);

		// then
		assertThat(acceptResponse.getWorkspaceCode()).isEqualTo(workspaceCode);
		verify(invitationRepository, times(1)).save(eq(invitation));

	}

	@Test
	@DisplayName("초대가 성공하면 초대의 상태가 ACCEPTED로 변경된다")
	void test2() {
		// given
		String workspaceCode = "testcode";
		String loginId = "user123";
		String email = "user123@test.com";

		Workspace workspace = testFixture.createWorkspace(workspaceCode);
		Member member = testFixture.createMember(loginId, email);
		WorkspaceMember workspaceMember = testFixture.createUserWorkspaceMember(member, workspace);
		LoginMemberDto loginMember = testFixture.createLoginMemberDto(loginId, email);
		Invitation invitation = testFixture.createPendingInvitation(workspace, member);

		when(invitationRepository.findByWorkspaceCodeAndMemberLoginId(workspaceCode, loginId)).thenReturn(
			Optional.of(invitation));
		when(invitationRepository.save(eq(invitation))).thenReturn(invitation);
		when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(workspaceMember);

		// when
		invitationService.acceptInvitation(loginMember, workspaceCode);
		Optional<Invitation> acceptedInvitation = invitationRepository.findByWorkspaceCodeAndMemberLoginId(
			workspaceCode, loginId);

		// then
		assertThat(acceptedInvitation).isPresent();
		assertThat(acceptedInvitation.get().getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
	}

	@Test
	@DisplayName("초대의 수락이 성공하면 해당 워크스페이스에 참여 된다")
	void test3() {
		// given
		String workspaceCode = "testcode";
		String loginId = "user123";
		String email = "user123@test.com";

		Workspace workspace = testFixture.createWorkspace(workspaceCode);
		Member member = testFixture.createMember(loginId, email);
		WorkspaceMember workspaceMember = testFixture.createUserWorkspaceMember(member, workspace);
		LoginMemberDto loginMember = testFixture.createLoginMemberDto(loginId, email);
		Invitation invitation = testFixture.createPendingInvitation(workspace, member);

		when(invitationRepository.findByWorkspaceCodeAndMemberLoginId(workspaceCode, loginId)).thenReturn(
			Optional.of(invitation));
		when(invitationRepository.save(eq(invitation))).thenReturn(invitation);
		when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(workspaceMember);

		// when
		InvitationAcceptResponse acceptResponse = invitationService.acceptInvitation(loginMember, workspaceCode);

		// then
		assertThat(acceptResponse.getWorkspaceCode()).isEqualTo(workspaceCode);
		verify(workspaceMemberRepository, times(1)).save(any(WorkspaceMember.class));
	}

	@Test
	@DisplayName("유효하지 않은 코드를 사용해서 초대를 수락하면 예외가 발생한다")
	void test4() {
		// given
		String workspaceCode = "invalidcode";
		String loginId = "user123";
		String email = "user123@test.com";

		LoginMemberDto loginMember = testFixture.createLoginMemberDto(loginId, email);

		when(invitationRepository.findByWorkspaceCodeAndMemberLoginId(workspaceCode, loginId)).thenReturn(
			Optional.empty());

		// when & then
		assertThatThrownBy(() -> invitationService.acceptInvitation(loginMember, workspaceCode)).isInstanceOf(
			InvitationNotFoundException.class);

	}

	@Test
	@DisplayName("존재하는 초대의 상태가 PENDING이 아니면 예외가 발생한다")
	void test5() {
		// given
		String workspaceCode = "invalidcode";
		String loginId = "user123";
		String email = "user123@test.com";

		Workspace workspace = testFixture.createWorkspace(workspaceCode);
		Member member = testFixture.createMember(loginId, email);
		LoginMemberDto loginMember = testFixture.createLoginMemberDto(loginId, email);
		Invitation invitation = testFixture.createAcceptedInvitation(workspace, member);

		when(invitationRepository.findByWorkspaceCodeAndMemberLoginId(workspaceCode, loginId))
			.thenReturn(Optional.of(invitation));

		// when & then
		assertThatThrownBy(() -> invitationService.acceptInvitation(loginMember, workspaceCode)).isInstanceOf(
			InvalidInvitationStatusException.class);

	}

	@Test
	@DisplayName("초대의 거절이 성공하면 초대 상태가 REJECTED로 변경된다")
	void test6() {
		// given
		String workspaceCode = "testcode";
		String loginId = "user123";
		String email = "user123@test.com";

		Workspace workspace = testFixture.createWorkspace(workspaceCode);
		Member member = testFixture.createMember(loginId, email);
		LoginMemberDto loginMember = testFixture.createLoginMemberDto(loginId, email);
		Invitation invitation = testFixture.createPendingInvitation(workspace, member);

		when(invitationRepository.findByWorkspaceCodeAndMemberLoginId(workspaceCode, loginId)).thenReturn(
			Optional.of(invitation));
		when(invitationRepository.save(eq(invitation))).thenReturn(invitation);

		// when
		invitationService.rejectInvitation(loginMember, workspaceCode);
		Optional<Invitation> rejectedInvitation = invitationRepository.findByWorkspaceCodeAndMemberLoginId(
			workspaceCode, loginId);

		// then
		assertThat(rejectedInvitation).isPresent();
		assertThat(rejectedInvitation.get().getStatus()).isEqualTo(InvitationStatus.REJECTED);
	}

}
