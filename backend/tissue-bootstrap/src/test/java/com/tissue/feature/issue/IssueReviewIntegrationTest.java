package com.tissue.feature.issue;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.application.service.IssueParticipantService;
import com.tissue.feature.issue.application.service.IssueReviewService;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Covers the review verdict and its optional feedback body, which is stored as a comment on the issue
 * rather than in a review-only table.
 */
@LLMGenerated(llmInvolvement = LLMInvolvement.ASSISTED, model = "claude-opus-5", generatedAt = "2026-08-23")
@Transactional
class IssueReviewIntegrationTest extends IntegrationTestSupport {

    private static final ProjectIdentifier PID = ProjectIdentifier.ofProjectKey("PROJ");

    @Autowired
    private IssueReviewService sut;

    @Autowired
    private IssueParticipantService participantService;

    @Autowired
    private IssueLifecycleService issueLifecycleService;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    private Member author;
    private Member reviewer;
    private String issueKey;

    @BeforeEach
    void setUp() {
        author = memberRepository.save(Member.create("author@tissue.com", "author", "Author"));
        reviewer = memberRepository.save(Member.create("reviewer@tissue.com", "reviewer", "Reviewer"));

        Project project = projectRepository.save(Project.create("PROJ", "Proj", null));
        projectMemberRepository.save(ProjectMember.createManager(project, author));
        projectMemberRepository.save(ProjectMember.create(project, reviewer));

        Workflow workflow = Workflow.create(Name.of("Default"), null, ColorType.ANSI_YELLOW);
        workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        workflowRepository.save(workflow);

        IssueType issueType = IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow);
        issueTypeRepository.save(issueType);

        em.flush();

        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .title("Login flow")
                .priority(IssuePriority.P2)
                .issueTypeId(issueType.getId())
                .customFields(Map.of())
                .build();
        issueKey = issueLifecycleService.create(PID, cmd, author.getId()).issueKey();

        IssueIdentifier iid = IssueIdentifier.ofIssueKey(issueKey);
        participantService.addReviewer(iid, reviewer.getId(), author.getId());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("success: feedback is stored as a comment stamped with the verdict")
    void feedbackBecomesAStampedComment() {
        // given
        IssueIdentifier iid = IssueIdentifier.ofIssueKey(issueKey);

        // when
        sut.submitReview(iid, false, "Rename the method before this ships.", reviewer.getId());
        em.flush();
        em.clear();

        // then
        List<Comment> comments = commentsOn(issueKey);
        assertThat(comments).hasSize(1);
        assertThat(comments.getFirst().getContent()).isEqualTo("Rename the method before this ships.");
        assertThat(comments.getFirst().getReviewStatus()).isEqualTo(ReviewStatus.CHANGES_REQUESTED);
        assertThat(comments.getFirst().isReview()).isTrue();
    }

    @Test
    @DisplayName("success: a review feedback comment is always a root comment, never a reply")
    void feedbackIsARootComment() {
        // given
        IssueIdentifier iid = IssueIdentifier.ofIssueKey(issueKey);

        // when
        sut.submitReview(iid, true, "Looks good.", reviewer.getId());
        em.flush();
        em.clear();

        // then
        assertThat(commentsOn(issueKey).getFirst().getParentComment()).isNull();
    }

    @Test
    @DisplayName("success: a review without feedback leaves only the status change")
    void noFeedbackLeavesNoComment() {
        // given
        IssueIdentifier iid = IssueIdentifier.ofIssueKey(issueKey);

        // when
        sut.submitReview(iid, true, null, reviewer.getId());
        em.flush();
        em.clear();

        // then
        assertThat(commentsOn(issueKey)).isEmpty();
        assertThat(reviewerStatus(issueKey)).isEqualTo(ReviewStatus.APPROVED);
    }

    @Test
    @DisplayName("success: a blank feedback body leaves only the status change")
    void blankFeedbackLeavesNoComment() {
        // given
        IssueIdentifier iid = IssueIdentifier.ofIssueKey(issueKey);

        // when
        sut.submitReview(iid, true, "   ", reviewer.getId());
        em.flush();
        em.clear();

        // then
        assertThat(commentsOn(issueKey)).isEmpty();
    }

    @Test
    @DisplayName("success: the verdict on the comment survives a re-review request that resets the reviewer")
    void verdictOnCommentSurvivesReviewReset() {
        // given
        IssueIdentifier iid = IssueIdentifier.ofIssueKey(issueKey);
        sut.submitReview(iid, true, "Looks good to me.", reviewer.getId());
        em.flush();
        em.clear();

        // when
        sut.requestReview(iid, Set.of(reviewer.getId()), author.getId());
        em.flush();
        em.clear();

        // then
        assertThat(reviewerStatus(issueKey)).isEqualTo(ReviewStatus.PENDING);
        assertThat(commentsOn(issueKey).getFirst().getReviewStatus()).isEqualTo(ReviewStatus.APPROVED);
    }

    @Test
    @DisplayName("success: each submitted review leaves its own entry, so the history is readable")
    void everySubmissionLeavesItsOwnEntry() {
        // given
        IssueIdentifier iid = IssueIdentifier.ofIssueKey(issueKey);
        sut.submitReview(iid, false, "Please add a test.", reviewer.getId());
        em.flush();
        em.clear();

        sut.requestReview(iid, Set.of(reviewer.getId()), author.getId());
        em.flush();
        em.clear();

        // when
        sut.submitReview(iid, true, "Test added, approving.", reviewer.getId());
        em.flush();
        em.clear();

        // then
        assertThat(commentsOn(issueKey))
                .extracting(Comment::getReviewStatus)
                .containsExactly(ReviewStatus.CHANGES_REQUESTED, ReviewStatus.APPROVED);
    }

    private List<Comment> commentsOn(String key) {
        return em.createQuery("SELECT c FROM Comment c WHERE c.issueKey = :key ORDER BY c.id ASC", Comment.class)
                .setParameter("key", key)
                .getResultList();
    }

    private ReviewStatus reviewerStatus(String key) {
        return em.createQuery("SELECT r.status FROM IssueReviewer r WHERE r.issueKey = :key", ReviewStatus.class)
                .setParameter("key", key)
                .getSingleResult();
    }
}
