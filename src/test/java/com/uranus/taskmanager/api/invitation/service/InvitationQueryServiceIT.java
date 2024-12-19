package com.uranus.taskmanager.api.invitation.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.domain.InvitationStatus;
import com.uranus.taskmanager.api.invitation.presentation.dto.InvitationSearchCondition;
import com.uranus.taskmanager.api.invitation.presentation.dto.response.InvitationResponse;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class InvitationQueryServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("사용자의 초대 목록을 상태로 필터링하여 조회할 수 있다")
	@Transactional
	void getInvitations() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"tester",
			"test@test.com",
			"test1234!"
		);

		Workspace workspace1 = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Workspace1",
			"Description1",
			"TESTCODE1",
			null
		);

		Workspace workspace2 = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Workspace2",
			"Description2",
			"TESTCODE2",
			null
		);

		Invitation pendingInvitation = invitationRepositoryFixture.createAndSaveInvitation(
			workspace1,
			member,
			InvitationStatus.PENDING
		);

		Invitation acceptedInvitation = invitationRepositoryFixture.createAndSaveInvitation(
			workspace2,
			member,
			InvitationStatus.ACCEPTED
		);

		InvitationSearchCondition searchCondition = new InvitationSearchCondition(
			List.of(InvitationStatus.PENDING, InvitationStatus.ACCEPTED)
		);
		Pageable pageable = PageRequest.of(
			0,
			20,
			Sort.by(Sort.Direction.DESC, "createdDate")
		);

		// when
		Page<InvitationResponse> result = invitationQueryService.getInvitations(
			member.getId(),
			searchCondition,
			pageable
		);

		// then
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent())
			.extracting("invitationId", "status")
			.contains(
				tuple(pendingInvitation.getId(), InvitationStatus.PENDING),
				tuple(acceptedInvitation.getId(), InvitationStatus.ACCEPTED)
			);
	}

	@Test
	@DisplayName("상태를 지정하지 않으면 PENDING 상태의 초대만 조회된다")
	@Transactional
	void getInvitationsDefaultStatus() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"tester",
			"test@test.com",
			"test1234!"
		);

		Workspace workspace1 = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Workspace1",
			"Description1",
			"TESTCODE1",
			null
		);

		Workspace workspace2 = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Workspace2",
			"Description2",
			"TESTCODE2",
			null
		);

		Invitation pendingInvitation = invitationRepositoryFixture.createAndSaveInvitation(
			workspace1,
			member,
			InvitationStatus.PENDING
		);

		invitationRepositoryFixture.createAndSaveInvitation(
			workspace2,
			member,
			InvitationStatus.ACCEPTED
		);

		InvitationSearchCondition searchCondition = new InvitationSearchCondition();
		Pageable pageable = PageRequest.of(
			0,
			20,
			Sort.by(Sort.Direction.DESC, "createdDate")
		);

		// when
		Page<InvitationResponse> result = invitationQueryService.getInvitations(
			member.getId(),
			searchCondition,
			pageable
		);

		// then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).invitationId()).isEqualTo(pendingInvitation.getId());
		assertThat(result.getContent().get(0).status()).isEqualTo(InvitationStatus.PENDING);
	}

}
