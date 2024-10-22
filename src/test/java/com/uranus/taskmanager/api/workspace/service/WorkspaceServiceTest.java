package com.uranus.taskmanager.api.workspace.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.invitation.InvitationStatus;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.exception.InvitationAlreadyExistsException;
import com.uranus.taskmanager.api.invitation.repository.InvitationRepository;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMemberRequest;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceParticipateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMemberResponse;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceCreateResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceParticipateResponse;
import com.uranus.taskmanager.api.workspace.exception.InvalidWorkspacePasswordException;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.exception.MemberAlreadyParticipatingException;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.fixture.dto.LoginMemberDtoFixture;
import com.uranus.taskmanager.fixture.entity.InvitationEntityFixture;
import com.uranus.taskmanager.fixture.entity.MemberEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceMemberEntityFixture;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

	@InjectMocks
	private WorkspaceService workspaceService;

	@Mock
	private WorkspaceRepository workspaceRepository;
	@Mock
	private MemberRepository memberRepository;
	@Mock
	private WorkspaceMemberRepository workspaceMemberRepository;
	@Mock
	private InvitationRepository invitationRepository;
	@Mock
	private PasswordEncoder passwordEncoder;

	WorkspaceEntityFixture workspaceEntityFixture;
	MemberEntityFixture memberEntityFixture;
	WorkspaceMemberEntityFixture workspaceMemberEntityFixture;
	InvitationEntityFixture invitationEntityFixture;
	LoginMemberDtoFixture loginMemberDtoFixture;

	@BeforeEach
	public void setup() {
		workspaceEntityFixture = new WorkspaceEntityFixture();
		memberEntityFixture = new MemberEntityFixture();
		workspaceMemberEntityFixture = new WorkspaceMemberEntityFixture();
		invitationEntityFixture = new InvitationEntityFixture();
		loginMemberDtoFixture = new LoginMemberDtoFixture();
	}

	@Test
	@DisplayName("유효한 워크스페이스 코드로 워크스페이스를 조회하면, 워크스페이스를 반환한다")
	void test2() {
		String workspaceCode = "testcode";
		Workspace workspace = Workspace.builder()
			.code(workspaceCode)
			.name("Test Workspace")
			.description("Test Description")
			.build();

		when(workspaceRepository.findByCode(workspaceCode)).thenReturn(Optional.of(workspace));

		WorkspaceCreateResponse response = workspaceService.get(workspaceCode);

		assertThat(response).isNotNull();
		assertThat(response.getCode()).isEqualTo(workspaceCode);
		verify(workspaceRepository, times(1)).findByCode(workspaceCode);
	}

	@Test
	@DisplayName("유효하지 워크스페이스 코드로 워크스페이스를 조회하면, WorkspaceNotFoundException 발생")
	void test3() {
		String workspaceCode = "INVALIDCODE";

		when(workspaceRepository.findByCode(workspaceCode)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> workspaceService.get(workspaceCode)).isInstanceOf(WorkspaceNotFoundException.class);

		verify(workspaceRepository, times(1)).findByCode(workspaceCode);
	}

	@Test
	@DisplayName("초대가 성공하면 초대가 PENDING 상태로 저장된다")
	void test8() {
		// given
		String workspaceCode = "TESTCODE";
		String loginId = "user123";
		String email = "user123@test.com";

		String invitedLoginId = "inviteduser123";
		String invitedEmail = "inviteduser123@test.com";

		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		LoginMemberDto loginMember = loginMemberDtoFixture.createLoginMemberDto(loginId, email);

		Member invitedMember = memberEntityFixture.createMember(invitedLoginId, invitedEmail);

		InviteMemberRequest inviteMemberRequest = new InviteMemberRequest(invitedLoginId);
		String identifier = inviteMemberRequest.getMemberIdentifier();

		when(workspaceRepository.findByCode(workspaceCode)).thenReturn(Optional.of(workspace));

		when(memberRepository.findByLoginIdOrEmail(identifier, identifier)).thenReturn(Optional.of(invitedMember));

		// when
		InviteMemberResponse inviteMemberResponse = workspaceService.inviteMember(workspaceCode, inviteMemberRequest,
			loginMember);

		// then
		assertThat(inviteMemberResponse.getStatus()).isEqualTo(InvitationStatus.PENDING);
		verify(invitationRepository, times(1)).save(any(Invitation.class));
	}

	@Test
	@DisplayName("유효하지 않은 워크스페이스 코드로 멤버를 초대하면 WorkspaceNotFoundException이 발생한다")
	void test4() {
		// given
		String workspaceCode = "INVALIDCODE";
		String loginId = "user123";
		String email = "user123@test.com";

		String invitedLoginId = "inviteduser123";

		LoginMemberDto loginMember = loginMemberDtoFixture.createLoginMemberDto(loginId, email);

		InviteMemberRequest inviteMemberRequest = new InviteMemberRequest(invitedLoginId);

		when(workspaceRepository.findByCode(workspaceCode)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(
			() -> workspaceService.inviteMember(workspaceCode, inviteMemberRequest, loginMember))
			.isInstanceOf(WorkspaceNotFoundException.class);

	}

	@Test
	@DisplayName("유효하지 않은 멤버 식별자를 사용해서 멤버를 초대하면 MemberNotFoundException이 발생한다")
	void test5() {
		// given
		String workspaceCode = "TESTCODE";
		String loginId = "user123";
		String email = "user123@test.com";

		String invitedLoginId = "inviteduser123";

		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		LoginMemberDto loginMember = loginMemberDtoFixture.createLoginMemberDto(loginId, email);

		InviteMemberRequest inviteMemberRequest = new InviteMemberRequest(invitedLoginId);
		String identifier = inviteMemberRequest.getMemberIdentifier();

		when(workspaceRepository.findByCode(workspaceCode)).thenReturn(Optional.of(workspace));

		when(memberRepository.findByLoginIdOrEmail(identifier, identifier)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(
			() -> workspaceService.inviteMember(workspaceCode, inviteMemberRequest, loginMember))
			.isInstanceOf(MemberNotFoundException.class);
	}

	@Test
	@DisplayName("멤버를 다시 초대할 때 초대가 존재하고 초대 상태가 PENDING이면 InvitationAlreadyExistsException이 발생한다")
	void test6() {
		// given
		String workspaceCode = "TESTCODE";
		String loginId = "user123";
		String email = "user123@test.com";

		String invitedLoginId = "inviteduser123";
		String invitedEmail = "inviteduser123@test.com";

		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		LoginMemberDto loginMember = loginMemberDtoFixture.createLoginMemberDto(loginId, email);

		Member invitedMember = memberEntityFixture.createMember(invitedLoginId, invitedEmail);
		Invitation invitation = invitationEntityFixture.createPendingInvitation(workspace, invitedMember);

		InviteMemberRequest inviteMemberRequest = new InviteMemberRequest(invitedLoginId);
		String identifier = inviteMemberRequest.getMemberIdentifier();

		when(workspaceRepository.findByCode(workspaceCode)).thenReturn(Optional.of(workspace));

		when(memberRepository.findByLoginIdOrEmail(identifier, identifier)).thenReturn(Optional.of(invitedMember));

		when(invitationRepository.findByWorkspaceAndMember(workspace, invitedMember)).thenReturn(
			Optional.of(invitation));

		// when & then
		assertThatThrownBy(
			() -> workspaceService.inviteMember(workspaceCode, inviteMemberRequest, loginMember)).isInstanceOf(
			InvitationAlreadyExistsException.class);
	}

	@Test
	@DisplayName("해당 워크스페이스에 이미 참여하고 있는 멤버를 다시 초대하면 MemberAlreadyParticipatingException이 발생한다")
	void test7() {
		// given
		String workspaceCode = "TESTCODE";
		String loginId = "user123";
		String email = "user123@test.com";

		String invitedLoginId = "inviteduser123";
		String invitedEmail = "inviteduser123@test.com";

		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		LoginMemberDto loginMember = loginMemberDtoFixture.createLoginMemberDto(loginId, email);

		Member invitedMember = memberEntityFixture.createMember(invitedLoginId, invitedEmail);
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createUserWorkspaceMember(invitedMember,
			workspace);

		InviteMemberRequest inviteMemberRequest = new InviteMemberRequest(invitedLoginId);
		String identifier = inviteMemberRequest.getMemberIdentifier();

		when(workspaceRepository.findByCode(workspaceCode)).thenReturn(Optional.of(workspace));

		when(memberRepository.findByLoginIdOrEmail(identifier, identifier)).thenReturn(Optional.of(invitedMember));

		when(workspaceMemberRepository.findByMemberLoginIdAndWorkspaceCode(invitedLoginId, workspaceCode)).thenReturn(
			Optional.of(workspaceMember));

		// when & then
		assertThatThrownBy(
			() -> workspaceService.inviteMember(workspaceCode, inviteMemberRequest, loginMember))
			.isInstanceOf(MemberAlreadyParticipatingException.class);
	}

	@Test
	@DisplayName("다수의 멤버를 초대 시 모든 멤버의 초대를 성공하면 실패한 멤버의 리스트는 비어있다")
	void test9() {
		// given
		String workspaceCode = "TESTCODE";
		String member1Id = "member1";
		String member2Id = "member2";
		String member1Email = "member1@test.com";
		String member2Email = "member2@test.com";

		List<String> memberIdentifiers = List.of(member1Id, member2Id);
		InviteMembersRequest inviteMembersRequest = new InviteMembersRequest(memberIdentifiers);

		// Mock LoginMemberDto
		LoginMemberDto loginMember = loginMemberDtoFixture.createLoginMemberDto("inviter", "inviter@test.com");

		// Mock Workspace
		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		when(workspaceRepository.findByCode(workspaceCode)).thenReturn(Optional.of(workspace));

		// Mock Member
		Member member1 = memberEntityFixture.createMember(member1Id, member1Email);
		Member member2 = memberEntityFixture.createMember(member2Id, member2Email);
		when(memberRepository.findByLoginIdOrEmail(member1Id, member1Id)).thenReturn(Optional.of(member1));
		when(memberRepository.findByLoginIdOrEmail(member2Id, member2Id)).thenReturn(Optional.of(member2));

		// Mock Invitation
		Invitation invitation1 = invitationEntityFixture.createPendingInvitation(workspace, member1);
		Invitation invitation2 = invitationEntityFixture.createPendingInvitation(workspace, member2);
		when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation1).thenReturn(invitation2);

		// when
		InviteMembersResponse response = workspaceService.inviteMembers(workspaceCode, inviteMembersRequest,
			loginMember);

		log.info("response = {}", response);

		// then
		assertThat(response.getInvitedMembers().size()).isEqualTo(2);
		assertThat(response.getFailedInvitedMembers()).isEmpty();

	}

	@Test
	@DisplayName("워크스페이스 참여가 성공하는 경우 참여 응답 DTO를 데이터로 제공하고, 참여 플래그는 False이다")
	void test10() {
		// given
		String workspaceCode = "TESTCODE";
		String loginId = "user123";
		String email = "user123@test.com";
		String workspacePassword = "workspace1234!";

		Workspace workspace = workspaceEntityFixture.createWorkspaceWithPassword(workspaceCode, workspacePassword);
		Member member = memberEntityFixture.createMember(loginId, email);
		LoginMemberDto loginMember = loginMemberDtoFixture.createLoginMemberDto(loginId, email);
		WorkspaceParticipateRequest request = new WorkspaceParticipateRequest(workspace.getPassword());

		when(workspaceRepository.findByCode(workspaceCode)).thenReturn(Optional.of(workspace));
		when(memberRepository.findByLoginId(loginMember.getLoginId())).thenReturn(Optional.of(member));
		when(workspaceMemberRepository.countByWorkspaceId(workspace.getId())).thenReturn(3);
		when(workspaceMemberRepository.findByMemberLoginIdAndWorkspaceCode(loginMember.getLoginId(), workspaceCode))
			.thenReturn(Optional.empty());
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

		// when
		WorkspaceParticipateResponse response = workspaceService.participateWorkspace(workspaceCode, request,
			loginMember);

		// then
		assertThat(response.isAlreadyMember()).isFalse();
		assertThat(response.getCode()).isEqualTo(workspaceCode);
		assertThat(response.getHeadcount()).isEqualTo(3);
	}

	@Test
	@DisplayName("워크스페이스 참여 시 이미 참여하고 있는 경우 정상 응답을 하되, 참여 플래그는 True이다")
	void test11() {
		// given
		String workspaceCode = "TESTCODE";
		String loginId = "user123";
		String email = "user123@test.com";
		String workspacePassword = "workspace1234!";

		Workspace workspace = workspaceEntityFixture.createWorkspaceWithPassword(workspaceCode, workspacePassword);
		Member member = memberEntityFixture.createMember(loginId, email);
		LoginMemberDto loginMember = loginMemberDtoFixture.createLoginMemberDto(loginId, email);
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createUserWorkspaceMember(member, workspace);
		WorkspaceParticipateRequest request = new WorkspaceParticipateRequest(workspace.getPassword());

		when(workspaceRepository.findByCode(workspaceCode)).thenReturn(Optional.of(workspace));
		when(memberRepository.findByLoginId(loginMember.getLoginId())).thenReturn(Optional.of(member));
		when(workspaceMemberRepository.countByWorkspaceId(workspace.getId())).thenReturn(3);
		when(workspaceMemberRepository.findByMemberLoginIdAndWorkspaceCode(loginMember.getLoginId(), workspaceCode))
			.thenReturn(Optional.of(workspaceMember));

		// when
		WorkspaceParticipateResponse response = workspaceService.participateWorkspace(workspaceCode, request,
			loginMember);

		// then
		assertThat(response).isNotNull();
		assertThat(response.isAlreadyMember()).isTrue();
		assertThat(response.getCode()).isEqualTo(workspaceCode);
		assertThat(response.getHeadcount()).isEqualTo(3);
	}

	@Test
	@DisplayName("워크스페이스 참여 시 비밀번호가 일치하지 않는 경우 예외가 발생한다")
	void test12() {
		// given
		String workspaceCode = "TESTCODE";
		String loginId = "user123";
		String email = "user123@test.com";
		String workspacePassword = "workspace1234!";
		String invalidPassword = "invalid1234!";

		Workspace workspace = workspaceEntityFixture.createWorkspaceWithPassword(workspaceCode, workspacePassword);
		Member member = memberEntityFixture.createMember(loginId, email);
		LoginMemberDto loginMember = loginMemberDtoFixture.createLoginMemberDto(loginId, email);
		WorkspaceParticipateRequest request = new WorkspaceParticipateRequest(invalidPassword);

		when(workspaceRepository.findByCode(workspaceCode)).thenReturn(Optional.of(workspace));
		when(memberRepository.findByLoginId(loginMember.getLoginId())).thenReturn(Optional.of(member));
		when(workspaceMemberRepository.findByMemberLoginIdAndWorkspaceCode(loginMember.getLoginId(), workspaceCode))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> workspaceService.participateWorkspace(workspaceCode, request,
			loginMember)).isInstanceOf(InvalidWorkspacePasswordException.class);
	}
}
