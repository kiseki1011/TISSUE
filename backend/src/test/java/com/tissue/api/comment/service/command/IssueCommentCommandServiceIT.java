package com.tissue.api.comment.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.comment.domain.Comment;
import com.tissue.api.comment.domain.enums.CommentStatus;
import com.tissue.api.comment.presentation.dto.request.CreateIssueCommentRequest;
import com.tissue.api.comment.presentation.dto.request.UpdateIssueCommentRequest;
import com.tissue.api.comment.presentation.dto.response.IssueCommentResponse;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.presentation.dto.request.create.CreateStoryRequest;
import com.tissue.api.issue.presentation.dto.response.create.CreateStoryResponse;
import com.tissue.api.member.presentation.dto.response.SignupMemberResponse;
import com.tissue.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.tissue.helper.ServiceIntegrationTestHelper;

class IssueCommentCommandServiceIT extends ServiceIntegrationTestHelper {

	String workspaceCode;
	String issueKey;

	@BeforeEach
	void setUp() {
		// 멤버 생성
		SignupMemberResponse testUser = memberFixture.createMember("testuser", "test@test.com");
		SignupMemberResponse testUser2 = memberFixture.createMember("testuser2", "test2@test.com");
		SignupMemberResponse testUser3 = memberFixture.createMember("testuser3", "test3@test.com");

		// 워크스페이스 생성
		CreateWorkspaceResponse createWorkspace = workspaceFixture.createWorkspace(testUser.memberId());

		workspaceCode = createWorkspace.code();

		// 멤버가 워크스페이스에 참가
		workspaceParticipationCommandService.joinWorkspace(workspaceCode, testUser2.memberId());
		workspaceParticipationCommandService.joinWorkspace(workspaceCode, testUser3.memberId());

		// Story 타입 이슈 생성
		CreateStoryRequest createStoryRequest = CreateStoryRequest.builder()
			.title("Test Story")
			.content("Test Story")
			.userStory("Test Story User Story")
			.build();

		CreateStoryResponse response = (CreateStoryResponse)issueCommandService.createIssue(
			workspaceCode,
			createStoryRequest
		);

		issueKey = response.issueKey();
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("특정 이슈에 댓글 작성을 성공한다")
	void createIssueComment_success() {
		// given
		Long currentWorkspaceMemberId = 1L;

		CreateIssueCommentRequest request = new CreateIssueCommentRequest(
			"Test Comment",
			null
		);

		// when
		IssueCommentResponse response = issueCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			request,
			currentWorkspaceMemberId
		);

		// then
		assertThat(response.author().workspaceMemberId()).isEqualTo(currentWorkspaceMemberId);
		assertThat(response.content()).isEqualTo("Test Comment");
	}

	@Test
	@DisplayName("댓글에 대한 대댓글 작성에 성공한다")
	void createIssueReplyComment_success() {
		// given
		Long currentWorkspaceMemberId = 1L;

		CreateIssueCommentRequest firstCommentRequest = new CreateIssueCommentRequest(
			"Test Comment",
			null
		);

		IssueCommentResponse firstCommentResponse = issueCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			firstCommentRequest,
			currentWorkspaceMemberId
		);

		CreateIssueCommentRequest replyCommentRequest = new CreateIssueCommentRequest(
			"Reply Comment",
			firstCommentResponse.id()
		);

		// when
		IssueCommentResponse response = issueCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			replyCommentRequest,
			currentWorkspaceMemberId
		);

		// then
		assertThat(response.content()).isEqualTo("Reply Comment");
		assertThat(response.author().workspaceMemberId()).isEqualTo(currentWorkspaceMemberId);
	}

	@Test
	@DisplayName("대댓글에 대한 대댓글 작성을 시도하면 예외가 발생한다")
	void createReplyComment_ofReplyComment_throwsException() {
		// given
		Long currentWorkspaceMemberId = 1L;

		CreateIssueCommentRequest parentCommentRequest = new CreateIssueCommentRequest(
			"Test Comment",
			null
		);

		IssueCommentResponse parentCommentResponse = issueCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			parentCommentRequest,
			currentWorkspaceMemberId
		);

		CreateIssueCommentRequest childCommentRequest = new CreateIssueCommentRequest(
			"Reply Comment",
			parentCommentResponse.id()
		);

		IssueCommentResponse childCommentResponse = issueCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			childCommentRequest,
			currentWorkspaceMemberId
		);

		// when & then
		CreateIssueCommentRequest request = new CreateIssueCommentRequest(
			"Reply comment of reply comment",
			childCommentResponse.id()
		);

		assertThatThrownBy(() -> issueCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			request,
			currentWorkspaceMemberId
		)).isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@DisplayName("댓글 작성자는 자신의 댓글을 수정할 수 있다")
	void updateComment_byAuthor_success() {
		// given
		Long currentWorkspaceMemberId = 1L;

		CreateIssueCommentRequest createCommentRequest = new CreateIssueCommentRequest(
			"Test Comment",
			null
		);

		IssueCommentResponse createCommentResponse = issueCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			createCommentRequest,
			currentWorkspaceMemberId
		);

		UpdateIssueCommentRequest request = new UpdateIssueCommentRequest("Update Comment");

		// when
		IssueCommentResponse response = issueCommentCommandService.updateComment(
			workspaceCode,
			issueKey,
			createCommentResponse.id(),
			request,
			currentWorkspaceMemberId
		);

		// then
		assertThat(response.author().workspaceMemberId()).isEqualTo(currentWorkspaceMemberId);
		assertThat(response.content()).isEqualTo("Update Comment");
	}

	@Test
	@DisplayName("댓글 작성자가 아니지만 댓글 수정을 시도하는 경우 예외가 발생한다")
	void updateComment_notAuthor_throwsException() {
		// given
		Long currentWorkspaceMemberId = 1L;
		Long notAuthorWorkspaceMemberId = 2L;

		CreateIssueCommentRequest createCommentRequest = new CreateIssueCommentRequest(
			"Test Comment",
			null
		);

		IssueCommentResponse createCommentResponse = issueCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			createCommentRequest,
			currentWorkspaceMemberId
		);

		UpdateIssueCommentRequest request = new UpdateIssueCommentRequest("Update Comment");

		// when & then
		assertThatThrownBy(() -> issueCommentCommandService.updateComment(
			workspaceCode,
			issueKey,
			createCommentResponse.id(),
			request,
			notAuthorWorkspaceMemberId
		)).isInstanceOf(ForbiddenOperationException.class);
	}

	@Test
	@DisplayName("댓글을 삭제하면 삭제된 댓글의 상태는 DELETED로 변한다")
	void deleteComment_success_commentStatusBecomesDELETED() {
		// given
		Long currentWorkspaceMemberId = 1L;

		CreateIssueCommentRequest createCommentRequest = new CreateIssueCommentRequest(
			"Test Comment",
			null
		);

		IssueCommentResponse createCommentResponse = issueCommentCommandService.createComment(
			workspaceCode,
			issueKey,
			createCommentRequest,
			currentWorkspaceMemberId
		);

		// when
		issueCommentCommandService.deleteComment(
			workspaceCode,
			issueKey,
			createCommentResponse.id(),
			currentWorkspaceMemberId
		);

		// then
		Comment comment = commentRepository.findById(createCommentResponse.id()).get();
		assertThat(comment.getStatus()).isEqualTo(CommentStatus.DELETED);
	}
}