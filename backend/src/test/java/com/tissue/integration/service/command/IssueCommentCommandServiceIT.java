package com.tissue.integration.service.command;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.comment.domain.model.Comment;
import com.tissue.api.comment.domain.model.IssueComment;
import com.tissue.api.comment.domain.model.enums.CommentStatus;
import com.tissue.api.comment.presentation.dto.request.CreateIssueCommentRequest;
import com.tissue.api.comment.presentation.dto.request.UpdateIssueCommentRequest;
import com.tissue.api.comment.presentation.dto.response.IssueCommentResponse;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.model.enums.IssuePriority;
import com.tissue.api.issue.domain.model.types.Story;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

class IssueCommentCommandServiceIT extends ServiceIntegrationTestHelper {

	Workspace workspace;
	Story issue;
	WorkspaceMember owner;
	WorkspaceMember workspaceMember1;
	WorkspaceMember workspaceMember2;

	@BeforeEach
	void setUp() {

		workspace = testDataFixture.createWorkspace(
			"test workspace",
			null,
			null
		);

		Member ownerMember = testDataFixture.createMember("owner");
		Member member1 = testDataFixture.createMember("member1");
		Member member2 = testDataFixture.createMember("member2");

		owner = testDataFixture.createWorkspaceMember(
			ownerMember,
			workspace,
			WorkspaceRole.OWNER
		);
		workspaceMember1 = testDataFixture.createWorkspaceMember(
			member1,
			workspace,
			WorkspaceRole.MEMBER
		);
		workspaceMember2 = testDataFixture.createWorkspaceMember(
			member2,
			workspace,
			WorkspaceRole.MEMBER
		);

		issue = testDataFixture.createStory(
			workspace,
			"story issue",
			IssuePriority.MEDIUM,
			LocalDateTime.now().plusDays(7)
		);
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("워크스페이스 멤버는 이슈에 댓글을 작성할 수 있다")
	void workspaceMemberShouldBeAbleToCreateComment() {
		// given
		CreateIssueCommentRequest request = new CreateIssueCommentRequest(
			"test comment",
			null
		);

		// when
		IssueCommentResponse response = issueCommentCommandService.createComment(
			workspace.getCode(),
			issue.getIssueKey(),
			request,
			workspaceMember1.getId()
		);

		// then
		assertThat(response.workspaceCode()).isEqualTo(workspace.getCode());
		assertThat(response.issueKey()).isEqualTo(issue.getIssueKey());

		Comment comment = commentRepository.findById(1L).get();
		assertThat(response.commentId()).isEqualTo(comment.getId());
	}

	@Test
	@DisplayName("이슈 댓글에 대한 대댓글을 작성할 수 있다")
	void canCreateReplyCommentUpToOneDepth() {
		// given
		IssueComment parentComment = testDataFixture.createIssueComment(
			issue,
			"original comment",
			workspaceMember1,
			null
		);

		CreateIssueCommentRequest replyCommentRequest = new CreateIssueCommentRequest(
			"reply comment",
			parentComment.getId()
		);

		// when
		IssueCommentResponse response = issueCommentCommandService.createComment(
			workspace.getCode(),
			issue.getIssueKey(),
			replyCommentRequest,
			workspaceMember1.getId()
		);

		// then
		assertThat(response.workspaceCode()).isEqualTo(workspace.getCode());
		assertThat(response.issueKey()).isEqualTo(issue.getIssueKey());

		Comment comment = commentRepository.findById(2L).get();
		assertThat(response.commentId()).isEqualTo(comment.getId());
		assertThat(comment.getContent()).isEqualTo("reply comment");
	}

	@Test
	@DisplayName("대댓글에 대한 대댓글 작성을 시도하면 예외가 발생한다")
	void shouldNotAllowCommentDepthExceedingOneLevel() {
		// given
		IssueComment parentComment = testDataFixture.createIssueComment(
			issue,
			"original comment",
			workspaceMember1,
			null
		);

		IssueComment childComment = testDataFixture.createIssueComment(
			issue,
			"reply comment",
			workspaceMember1,
			parentComment
		);

		// when & then
		CreateIssueCommentRequest request = new CreateIssueCommentRequest(
			"reply comment of reply comment",
			childComment.getId()
		);

		assertThatThrownBy(() -> issueCommentCommandService.createComment(
			workspace.getCode(),
			issue.getIssueKey(),
			request,
			workspaceMember1.getId()
		)).isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@DisplayName("댓글 작성자는 자신의 댓글을 수정할 수 있다")
	void commentAuthorCanEditOwnComment() {
		// given
		IssueComment comment = testDataFixture.createIssueComment(
			issue,
			"test comment",
			workspaceMember1,
			null
		);

		UpdateIssueCommentRequest request = new UpdateIssueCommentRequest("update comment");

		// when
		IssueCommentResponse response = issueCommentCommandService.updateComment(
			workspace.getCode(),
			issue.getIssueKey(),
			comment.getId(),
			request,
			workspaceMember1.getId()
		);

		// then
		assertThat(response.workspaceCode()).isEqualTo(workspace.getCode());
		assertThat(response.issueKey()).isEqualTo(issue.getIssueKey());

		Comment updatedComment = commentRepository.findById(1L).get();

		assertThat(response.commentId()).isEqualTo(updatedComment.getId());
		assertThat(updatedComment.getContent()).isEqualTo("update comment");
	}

	@Test
	@DisplayName("댓글 작성자가 아니지만 댓글 수정을 시도하는 경우 예외가 발생한다")
	void ifNotAuthorCannotEditComment() {
		// given
		IssueComment comment = testDataFixture.createIssueComment(
			issue,
			"test comment",
			workspaceMember1,
			null
		);

		UpdateIssueCommentRequest request = new UpdateIssueCommentRequest("update comment");

		// when & then
		assertThatThrownBy(() -> issueCommentCommandService.updateComment(
			workspace.getCode(),
			issue.getIssueKey(),
			comment.getId(),
			request,
			workspaceMember2.getId() // not author of the comment
		)).isInstanceOf(ForbiddenOperationException.class);
	}

	@Test
	@DisplayName("댓글을 삭제하면 삭제된 댓글의 상태는 DELETED로 변한다")
	void ifCommentIsDeletedCommentStatusChangedToDeleted() {
		// given
		IssueComment comment = testDataFixture.createIssueComment(
			issue,
			"test comment",
			workspaceMember1,
			null
		);

		// when
		issueCommentCommandService.deleteComment(
			workspace.getCode(),
			issue.getIssueKey(),
			comment.getId(),
			workspaceMember1.getId()
		);

		// then
		Comment findComment = commentRepository.findById(comment.getId()).get();
		assertThat(findComment.getStatus()).isEqualTo(CommentStatus.DELETED);
	}
}