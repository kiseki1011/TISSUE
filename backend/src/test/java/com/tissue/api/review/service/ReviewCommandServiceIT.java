package com.tissue.api.review.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.issue.presentation.dto.response.create.CreateStoryResponse;
import com.tissue.api.member.presentation.dto.response.SignupMemberResponse;
import com.tissue.api.review.exception.DuplicateReviewerException;
import com.tissue.api.review.presentation.dto.response.AddReviewerResponse;
import com.tissue.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;
import com.tissue.helper.ServiceIntegrationTestHelper;

class ReviewCommandServiceIT extends ServiceIntegrationTestHelper {

	String workspaceCode;
	String issueKey;

	@BeforeEach
	public void setUp() {
		// 테스트 멤버 testuser, testuser2 추가
		SignupMemberResponse testUser = memberFixture.createMember("testuser", "test@test.com");
		SignupMemberResponse testUser2 = memberFixture.createMember("testuser2", "test2@test.com");

		// testuser가 생성한 테스트 워크스페이스 추가
		CreateWorkspaceResponse createWorkspace = workspaceFixture.createWorkspace(testUser.memberId());

		workspaceCode = createWorkspace.code();

		// testUser2를 테스트 워크스페이스에 참가
		workspaceParticipationCommandService.joinWorkspace(workspaceCode, testUser2.memberId());

		// 테스트 워크스페이스에 Story 추가
		CreateStoryResponse createdStory = (CreateStoryResponse)issueFixture.createStory(
			workspaceCode,
			"Test Story",
			null);

		issueKey = createdStory.issueKey();
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("리뷰어 추가에 성공하면 AddReviewerResponse를 반환한다")
	void addReviewer_success_returnAddReviewerResponse() {
		// given
		WorkspaceMember workspaceMember = workspaceMemberRepository.findByMemberIdAndWorkspaceCode(2L, workspaceCode)
			.orElseThrow();

		// when
		AddReviewerResponse response = reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			workspaceMember.getId());

		// then
		assertThat(response.reviewerId()).isEqualTo(workspaceMember.getId());
		assertThat(response.reviewerNickname()).isEqualTo(workspaceMember.getNickname());
	}

	@Test
	@DisplayName("워크스페이스에 존재하지 않는 멤버를 리뷰어로 추가하려고 시도하면 예외가 발생한다")
	void addReviewer_thatIsNotInWorkspace_throwsException() {
		// given
		Long invalidWorkspaceMemberId = 999L;

		// when & then
		assertThatThrownBy(() -> reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			invalidWorkspaceMemberId))
			.isInstanceOf(WorkspaceMemberNotFoundException.class);
	}

	@Test
	@DisplayName("이미 리뷰어로 등록된 멤버를 리뷰어로 추가하려고 시도하면 예외가 발생한다")
	void addReviewer_thatIsAlreadyReviewer_throwsException() {
		// given
		WorkspaceMember workspaceMember = workspaceMemberRepository.findByMemberIdAndWorkspaceCode(2L, workspaceCode)
			.orElseThrow();

		reviewCommandService.addReviewer(workspaceCode, issueKey, workspaceMember.getId());

		// when & then
		assertThatThrownBy(() -> reviewCommandService.addReviewer(
			workspaceCode,
			issueKey,
			workspaceMember.getId()))
			.isInstanceOf(DuplicateReviewerException.class);
	}

}