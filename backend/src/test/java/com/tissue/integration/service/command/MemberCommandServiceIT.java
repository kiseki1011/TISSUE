package com.tissue.integration.service.command;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.member.domain.JobType;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.vo.Name;
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberEmailRequest;
import com.tissue.api.member.presentation.dto.request.UpdateMemberProfileRequest;
import com.tissue.api.member.presentation.dto.response.command.MemberResponse;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

class MemberCommandServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("회원 가입을 할 수 있다")
	void canSignup() {
		// given
		SignupMemberRequest request = SignupMemberRequest.builder()
			.loginId("tester")
			.email("test@test.com")
			.password("test1234!")
			.biography("biography")
			.jobType(JobType.DEVELOPER)
			.firstName("Gildong")
			.lastName("Hong")
			.birthDate(LocalDate.of(1900, 1, 1))
			.build();

		// when
		MemberResponse response = memberCommandService.signup(request);

		// then
		Member foundMember = findMemberById(1L);

		assertThat(foundMember.getId()).isEqualTo(response.memberId());
	}

	@Test
	@DisplayName("회원 가입에 성공하여 저장된 멤버의 패스워드는 암호화 되어 있다")
	void signupMemberPasswordIsEncrypted() {
		// given
		SignupMemberRequest request = SignupMemberRequest.builder()
			.loginId("tester")
			.email("test@test.com")
			.password("test1234!")
			.biography("biography")
			.jobType(JobType.DEVELOPER)
			.firstName("Gildong")
			.lastName("Hong")
			.birthDate(LocalDate.of(1900, 1, 1))
			.build();

		// when
		memberCommandService.signup(request);

		// then
		String encodedPassword = memberRepository.findByLoginId("tester").get().getPassword();

		assertThat(passwordEncoder.matches(request.password(), encodedPassword)).isTrue();
	}

	@Test
	@DisplayName("회원 가입 시 제공한 패스워드와 암호화 되어 저장된 패스워드는 서로 다르다")
	void requestPaswordForSignupMustBeDifferentWithSavedEncryptedPassword() {
		// given
		SignupMemberRequest request = SignupMemberRequest.builder()
			.loginId("tester")
			.email("test@test.com")
			.password("test1234!")
			.biography("biography")
			.jobType(JobType.DEVELOPER)
			.firstName("Gildong")
			.lastName("Hong")
			.birthDate(LocalDate.of(1900, 1, 1))
			.build();

		// when
		memberCommandService.signup(request);

		// then
		String encodedPassword = memberRepository.findByLoginId("tester").get().getPassword();

		assertThat(encodedPassword).isNotEqualTo(request.password());
	}

	@Test
	@DisplayName("멤버의 이메일(email)을 업데이트할 수 있다")
	void canUpdateMemberEmail() {
		// given
		Member member = testDataFixture.createMember("tester");
		String originalEmail = member.getEmail();

		// when
		memberCommandService.updateEmail(
			new UpdateMemberEmailRequest("test1234!", "newemail@test.com"),
			member.getId()
		);

		// then
		Member updatedMember = findMemberById(member.getId());

		assertThat(updatedMember.getEmail()).isEqualTo("newemail@test.com");
		assertThat(updatedMember.getEmail()).isNotEqualTo(originalEmail);
	}

	@Test
	@DisplayName("이미 가입된 이메일(중복된 이메일)로 이메일을 업데이트할 수 없다")
	void cannotUpdateEmailToDuplicateEmail() {
		// given
		Member member = testDataFixture.createMember("tester");

		// when & then
		assertThatThrownBy(() -> memberCommandService.updateEmail(
			new UpdateMemberEmailRequest("test1234!", member.getEmail()),
			member.getId()
		))
			.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	@DisplayName("멤버 탈퇴가 가능하다")
	void canWithdrawMember() {
		// given
		Member member = testDataFixture.createMember("tester");

		// when
		memberCommandService.withdraw(member.getId());

		// then
		assertThat(memberRepository.findById(member.getId())).isEmpty();
	}

	@Test
	@DisplayName("워크스페이스의 소유자(OWNER)로 등록되어 있는 경우 탈퇴가 불가능하다")
	void cannotWithdrawMember_IfMemberIsOwnerOfWorkspace() {
		// given
		Member member = testDataFixture.createMember("tester");

		Workspace workspace = testDataFixture.createWorkspace("workspace 1", null, null);

		// add member as owner of workspace
		WorkspaceMember workspaceMember = testDataFixture.createWorkspaceMember(member, workspace, WorkspaceRole.OWNER);

		// when & then
		assertThatThrownBy(() -> memberCommandService.withdraw(member.getId()))
			.isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@DisplayName("멤버의 상세 정보(profile)를 업데이트할 수 있다")
	void canUpdateMemberProfile() {
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

		UpdateMemberProfileRequest request = UpdateMemberProfileRequest.builder()
			.biography("Im currently unemployed")
			.jobType(JobType.ETC)
			.birthDate(LocalDate.of(1995, 2, 2))
			.build();

		// when
		MemberResponse response = memberCommandService.updateInfo(request, member.getId());

		// then
		assertThat(response.memberId()).isEqualTo(member.getId());
		assertThat(findMemberById(member.getId()).getJobType()).isEqualTo(JobType.ETC);
	}

	@Test
	@DisplayName("멤버 상세 정보(profile) 업데이트 시, 요청의 null인 필드는 변경 대상에서 제외된다")
	void whenUpdateMemberProfile_NullFieldsInRequestAreExcludedFromUpdate() {
		// given
		Member member = memberRepository.save(Member.builder()
			.loginId("tester")
			.email("test@test.com")
			.password("test1234!")
			.name(Name.builder()
				.firstName("Gildong")
				.lastName("Hong")
				.build())
			.biography("Im a backend developer!")
			.jobType(JobType.DEVELOPER)
			.birthDate(LocalDate.of(1995, 1, 1))
			.build());

		UpdateMemberProfileRequest request = UpdateMemberProfileRequest.builder()
			.biography("Im currently unemployed")
			.build();

		// when
		MemberResponse response = memberCommandService.updateInfo(request, member.getId());

		// then
		assertThat(response.memberId()).isEqualTo(member.getId());

		assertThat(findMemberById(member.getId()).getBiography()).isEqualTo("Im currently unemployed");
		assertThat(findMemberById(member.getId()).getJobType()).isEqualTo(JobType.DEVELOPER);
		assertThat(findMemberById(member.getId()).getBirthDate()).isEqualTo(LocalDate.of(1995, 1, 1));
	}

	@Test
	@DisplayName("멤버 상세 정보(profile) 업데이트 시, firstName 또는 lastName 둘 중 하나라도 비어있으면 이름은 변경되지 않는다")
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

		UpdateMemberProfileRequest request = UpdateMemberProfileRequest.builder()
			.lastName("Kim")
			.build();

		// when
		MemberResponse response = memberCommandService.updateInfo(request, member.getId());

		// then
		assertThat(response.memberId()).isEqualTo(member.getId());
		assertThat(findMemberById(member.getId()).getName().getLastName()).isEqualTo("Hong");
	}

	private Member findMemberById(Long id) {
		return memberRepository.findById(id).get();
	}
}
