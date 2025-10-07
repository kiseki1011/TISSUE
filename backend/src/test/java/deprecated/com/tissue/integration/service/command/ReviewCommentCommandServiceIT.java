package deprecated.com.tissue.integration.service.command;

import deprecated.com.tissue.support.helper.ServiceIntegrationTestHelper;

class ReviewCommentCommandServiceIT extends ServiceIntegrationTestHelper {

	// Workspace workspace;
	// Review review;
	// WorkspaceMember owner;
	// WorkspaceMember workspaceMember1;
	// WorkspaceMember workspaceMember2;
	//
	// @Transactional
	// @BeforeEach
	// void setUp() {
	// 	workspace = testDataFixture.createWorkspace("test workspace", null, null);
	//
	// 	Member ownerMember = testDataFixture.createMember("owner");
	// 	Member member1 = testDataFixture.createMember("member1");
	// 	Member member2 = testDataFixture.createMember("member2");
	//
	// 	owner = testDataFixture.createWorkspaceMember(
	// 		ownerMember,
	// 		workspace,
	// 		WorkspaceRole.OWNER
	// 	);
	// 	workspaceMember1 = testDataFixture.createWorkspaceMember(
	// 		member1,
	// 		workspace,
	// 		WorkspaceRole.MEMBER
	// 	);
	// 	workspaceMember2 = testDataFixture.createWorkspaceMember(
	// 		member2,
	// 		workspace,
	// 		WorkspaceRole.MEMBER
	// 	);
	//
	// 	issue = testDataFixture.createStory(
	// 		workspace,
	// 		"story issue",
	// 		IssuePriority.MEDIUM,
	// 		LocalDateTime.now().plusDays(7)
	// 	);
	//
	// 	// add workspaceMember1 as issue assignee
	// 	testDataFixture.addIssueAssignee(issue, workspaceMember1);
	//
	// 	// add workspaceMember2 as issue reviewer
	// 	IssueReviewer reviewer = testDataFixture.addIssueReviewer(issue, workspaceMember2);
	//
	// 	// update issue status to IN_PROGRESS
	// 	issue.updateStatus(IssueStatus.IN_PROGRESS);
	//
	// 	// request review of issue
	// 	issue.requestReview();
	//
	// 	// add a APPROVED review
	// 	review = testDataFixture.createReview(reviewer, "test review", ReviewStatus.APPROVED);
	// }
	//
	// @AfterEach
	// public void tearDown() {
	// 	databaseCleaner.execute();
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("특정 리뷰에 댓글 작성을 성공한다")
	// void createReviewComment_success() {
	// 	// given
	// 	CreateReviewCommentRequest request = new CreateReviewCommentRequest(
	// 		"Test Comment",
	// 		null
	// 	);
	//
	// 	// when
	// 	ReviewCommentResponse response = reviewCommentCommandService.createComment(
	// 		workspace.getCode(),
	// 		issue.getIssueKey(),
	// 		review.getId(),
	// 		request,
	// 		workspaceMember1.getId()
	// 	);
	//
	// 	// then
	// 	Comment comment = commentRepository.findById(1L).get();
	//
	// 	assertThat(comment.getContent()).isEqualTo("Test Comment");
	// 	assertThat(response.commentId()).isEqualTo(comment.getId());
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("리뷰 댓글에 대한 대댓글 작성에 성공한다")
	// void createReviewReplyComment_success() {
	// 	// given
	// 	ReviewComment parentComment = testDataFixture.createReviewComment(
	// 		review,
	// 		"original comment",
	// 		workspaceMember1,
	// 		null
	// 	);
	//
	// 	CreateReviewCommentRequest replyCommentRequest = new CreateReviewCommentRequest(
	// 		"reply comment",
	// 		parentComment.getId()
	// 	);
	//
	// 	// when
	// 	ReviewCommentResponse response = reviewCommentCommandService.createComment(
	// 		workspace.getCode(),
	// 		issue.getIssueKey(),
	// 		review.getId(),
	// 		replyCommentRequest,
	// 		workspaceMember1.getId()
	// 	);
	//
	// 	// then
	// 	Comment comment = commentRepository.findById(2L).get();
	//
	// 	assertThat(comment.getContent()).isEqualTo("reply comment");
	// 	assertThat(response.commentId()).isEqualTo(comment.getId());
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("댓글 작성자는 자신의 댓글을 수정할 수 있다")
	// void updateReviewComment_byAuthor_success() {
	// 	// given
	// 	ReviewComment comment = testDataFixture.createReviewComment(
	// 		review,
	// 		"test comment",
	// 		workspaceMember1,
	// 		null
	// 	);
	//
	// 	UpdateReviewCommentRequest updateRequest = new UpdateReviewCommentRequest("update comment");
	//
	// 	// when
	// 	ReviewCommentResponse updateResponse = reviewCommentCommandService.updateComment(
	// 		workspace.getCode(),
	// 		issue.getIssueKey(),
	// 		review.getId(),
	// 		comment.getId(),
	// 		updateRequest,
	// 		workspaceMember1.getId()
	// 	);
	//
	// 	// then
	// 	assertThat(comment.getContent()).isEqualTo("update comment");
	// 	assertThat(updateResponse.commentId()).isEqualTo(comment.getId());
	// }
}