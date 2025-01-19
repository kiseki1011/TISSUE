package com.tissue.api.member.service.command;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.common.exception.DuplicateResourceException;
import com.tissue.api.common.exception.InvalidOperationException;
import com.tissue.api.member.domain.JobType;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.vo.Name;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberEmailRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberInfoRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberPasswordRequest;
import com.tissue.api.member.presentation.dto.response.SignupMemberResponse;
import com.tissue.api.member.presentation.dto.response.UpdateMemberEmailResponse;
import com.tissue.api.member.presentation.dto.response.UpdateMemberInfoResponse;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.helper.ServiceIntegrationTestHelper;

class MemberCommandServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("회원 가입에 성공하면 멤버가 저장된다")
	void signup_sucess_memberIsSaved() {
		// given
		SignupMemberRequest signupMemberRequest = signupRequestDtoFixture.createSignupRequest(
			"testuser",
			"testemail@test.com",
			"testpassword1234!"
		);

		// when
		SignupMemberResponse signupMemberResponse = memberCommandService.signup(signupMemberRequest);

		// then
		assertThat(signupMemberResponse.memberId()).isEqualTo(1L);
		assertThat(findMemberById(1L).getEmail()).isEqualTo("testemail@test.com");
	}

	@Test
	@DisplayName("회원 가입에 성공하여 저장된 멤버의 패스워드는 암호화 되어 있다")
	void signup_sucess_memberPasswordIsEncrypted() {
		// given
		SignupMemberRequest signupMemberRequest = signupRequestDtoFixture.createSignupRequest(
			"testuser",
			"testemail@test.com",
			"testpassword1234!"
		);

		// when
		memberCommandService.signup(signupMemberRequest);

		// then
		Optional<Member> member = memberRepository.findByLoginId("testuser");
		String encodedPassword = member.get().getPassword();

		assertThat(passwordEncoder.matches("testpassword1234!", encodedPassword)).isTrue();
	}

	@Test
	@DisplayName("회원 가입 시 입력한 패스워드와 암호화한 패스워드는 서로 다르다")
	void signup_sucess_requestPaswordMustBeDifferentWithEncryptedPassword() {
		// given
		SignupMemberRequest signupMemberRequest = signupRequestDtoFixture.createSignupRequest(
			"testuser",
			"testemail@test.com",
			"testpassword1234!"
		);

		// when
		memberCommandService.signup(signupMemberRequest);

		// then
		Optional<Member> member = memberRepository.findByLoginId("testuser");
		String encodedPassword = member.get().getPassword();

		assertThat(encodedPassword).isNotEqualTo("testpassword1234!");
	}

	@Test
	@DisplayName("이메일 업데이트를 성공하면 이메일 업데이트 응답을 반환한다")
	void updateEmail_success_returnsMemberEmailUpdateResponse() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		String newEmail = "newemail@test.com";
		UpdateMemberEmailRequest request = new UpdateMemberEmailRequest("password1234!", newEmail);

		// when
		UpdateMemberEmailResponse response = memberCommandService.updateEmail(request, member.getId());

		// then
		assertThat(response.memberId()).isEqualTo(member.getId());

		Member updatedMember = findMemberById(member.getId());
		assertThat(updatedMember.getEmail()).isEqualTo(newEmail);
	}

	@Test
	@DisplayName("이메일 업데이트 시 이메일이 중복되면 예외가 발생한다")
	void updateEmail_throwsException_whenEmailDuplicated() {
		// given
		Member existingMember = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		UpdateMemberEmailRequest request = new UpdateMemberEmailRequest("password1234!", existingMember.getEmail());

		// when & then
		assertThatThrownBy(() -> memberCommandService.updateEmail(request, existingMember.getId()))
			.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	@DisplayName("패스워드 업데이트를 성공하면 업데이트 된 멤버는 암호화된 새로운 패스워드를 가진다")
	void updatePassword_success_updatedMemberHasNewEncrytedPassword() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		String newPassword = "newpassword1234!";
		UpdateMemberPasswordRequest request = new UpdateMemberPasswordRequest("password1234!", newPassword);

		// when
		memberCommandService.updatePassword(request, member.getId());

		// then
		Member updatedMember = findMemberById(member.getId());
		assertThat(passwordEncoder.matches(newPassword, updatedMember.getPassword())).isTrue();
	}

	@Test
	@DisplayName("멤버 탈퇴에 성공하면 해당 멤버는 삭제된다")
	void withdrawMember_success_memberIsDeleted() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		// when
		memberCommandService.withdraw(member.getId());

		// then
		assertThat(memberRepository.findById(member.getId())).isEmpty();
	}

	@Test
	@DisplayName("멤버 탈퇴에 요청 시 워크스페이스 소유자(OWNER)로 등록되어 있으면 예외가 발생한다")
	void withdrawMember_throwsException_ifRequesterIsOwnerOfWorkspace() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		Workspace workspace = workspaceRepository.save(Workspace.builder()
			.code("TESTCODE")
			.name("workspace1")
			.description("description1")
			.build());

		workspaceMemberRepository.save(WorkspaceMember.addOwnerWorkspaceMember(member, workspace));

		// when & then
		assertThatThrownBy(() -> memberCommandService.withdraw(member.getId()))
			.isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@DisplayName("멤버 정보(프로필) 변경에 성공하면 변경된 멤버의 id가 포함된 응답을 반환한다")
	void testUpdateMemberInfo_ifSuccess_returnUpdateMemberInfoResponse() {
		// given
		Member member = memberRepository.save(Member.builder()
			.loginId("tester")
			.email("test@test.com")
			.password("test1234!")
			.name(Name.builder()
				.firstName("Gildong")
				.lastName("Hong")
				.build())
			.biography("Im a backend developer")
			.jobType(JobType.DEVELOPER)
			.birthDate(LocalDate.of(1995, 1, 1))
			.build());

		UpdateMemberInfoRequest request = UpdateMemberInfoRequest.builder()
			.biography("Im currently unemployed")
			.jobType(JobType.ETC)
			.birthDate(LocalDate.of(1995, 2, 2))
			.build();

		// when
		UpdateMemberInfoResponse response = memberCommandService.updateInfo(request, member.getId());

		// then
		assertThat(response.memberId()).isEqualTo(member.getId());
		assertThat(findMemberById(member.getId()).getJobType()).isEqualTo(JobType.ETC);
	}

	@Test
	@DisplayName("멤버 정보(프로필) 변경 요청 시, null인 필드는 변경이 일어나지 않는다")
	void testUpdateMemberInfo_requestNullFieldsNotChanged() {
		// given
		Member member = memberRepository.save(Member.builder()
			.loginId("tester")
			.email("test@test.com")
			.password("test1234!")
			.name(Name.builder()
				.firstName("Gildong")
				.lastName("Hong")
				.build())
			.biography("Im a backend developer")
			.jobType(JobType.DEVELOPER)
			.birthDate(LocalDate.of(1995, 1, 1))
			.build());

		UpdateMemberInfoRequest request = UpdateMemberInfoRequest.builder()
			.biography("Im currently unemployed")
			.build();

		// when
		UpdateMemberInfoResponse response = memberCommandService.updateInfo(request, member.getId());

		// then
		assertThat(response.memberId()).isEqualTo(member.getId());

		assertThat(findMemberById(member.getId()).getBiography()).isEqualTo("Im currently unemployed");
		assertThat(findMemberById(member.getId()).getJobType()).isEqualTo(JobType.DEVELOPER);
		assertThat(findMemberById(member.getId()).getBirthDate()).isEqualTo(LocalDate.of(1995, 1, 1));
	}

	@Test
	@DisplayName("멤버 정보(프로필) 변경 요청 시, firstName 또는 lastName 둘 중 하나라도 비어있으면 이름은 변경되지 않는다")
	void test() {
		// given
		Member member = memberRepository.save(Member.builder()
			.loginId("tester")
			.email("test@test.com")
			.password("test1234!")
			.name(Name.builder()
				.firstName("Gildong")
				.lastName("Hong")
				.build())
			.biography("Im a backend developer")
			.jobType(JobType.DEVELOPER)
			.birthDate(LocalDate.of(1995, 1, 1))
			.build());

		UpdateMemberInfoRequest request = UpdateMemberInfoRequest.builder()
			.lastName("Kim")
			.build();

		// when
		UpdateMemberInfoResponse response = memberCommandService.updateInfo(request, member.getId());

		// then
		assertThat(response.memberId()).isEqualTo(member.getId());
		assertThat(findMemberById(member.getId()).getName().getLastName()).isEqualTo("Hong");
	}

	private Member findMemberById(Long id) {
		return memberRepository.findById(id)
			.orElseThrow();
	}
}
