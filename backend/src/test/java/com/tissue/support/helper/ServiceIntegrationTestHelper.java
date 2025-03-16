package com.tissue.support.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.tissue.api.assignee.service.command.AssigneeCommandService;
import com.tissue.api.comment.domain.repository.CommentRepository;
import com.tissue.api.comment.service.command.IssueCommentCommandService;
import com.tissue.api.comment.service.command.ReviewCommentCommandService;
import com.tissue.api.invitation.domain.repository.InvitationRepository;
import com.tissue.api.invitation.service.command.InvitationCommandService;
import com.tissue.api.invitation.service.query.InvitationQueryService;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.service.command.IssueCommandService;
import com.tissue.api.issue.service.command.IssueRelationCommandService;
import com.tissue.api.issue.validator.checker.CircularDependencyChecker;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.service.command.MemberCommandService;
import com.tissue.api.member.service.query.MemberQueryService;
import com.tissue.api.member.validator.MemberValidator;
import com.tissue.api.position.domain.repository.PositionRepository;
import com.tissue.api.position.service.command.PositionCommandService;
import com.tissue.api.position.service.query.PositionQueryService;
import com.tissue.api.review.domain.repository.IssueReviewerRepository;
import com.tissue.api.review.domain.repository.ReviewRepository;
import com.tissue.api.review.service.command.ReviewCommandService;
import com.tissue.api.review.service.command.ReviewerCommandService;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.sprint.domain.repository.SprintQueryRepository;
import com.tissue.api.sprint.domain.repository.SprintRepository;
import com.tissue.api.sprint.service.command.SprintCommandService;
import com.tissue.api.sprint.service.command.SprintReader;
import com.tissue.api.sprint.service.query.SprintQueryService;
import com.tissue.api.team.domain.repository.TeamRepository;
import com.tissue.api.team.service.command.TeamCommandService;
import com.tissue.api.team.service.query.TeamQueryService;
import com.tissue.api.util.WorkspaceCodeParser;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.service.command.WorkspaceCommandService;
import com.tissue.api.workspace.service.command.WorkspaceReader;
import com.tissue.api.workspace.service.command.create.RetryCodeGenerationOnExceptionService;
import com.tissue.api.workspace.validator.WorkspaceValidator;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberCommandService;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberInviteService;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;
import com.tissue.api.workspacemember.service.command.WorkspaceParticipationCommandService;
import com.tissue.api.workspacemember.service.query.WorkspaceParticipationQueryService;
import com.tissue.support.fixture.TestDataFixture;
import com.tissue.support.util.DatabaseCleaner;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class ServiceIntegrationTestHelper {

	/**
	 * Common
	 */
	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected DatabaseCleaner databaseCleaner;
	@Autowired
	protected PasswordEncoder passwordEncoder;
	@Autowired
	protected WorkspaceCodeParser workspaceCodeParser;
	@Autowired
	protected EntityManager entityManager;

	/**
	 * Service
	 */
	@Autowired
	protected WorkspaceMemberCommandService workspaceMemberCommandService;
	@Autowired
	protected WorkspaceMemberInviteService workspaceMemberInviteService;
	@Autowired
	protected WorkspaceMemberReader workspaceMemberReader;
	@Autowired
	protected WorkspaceParticipationQueryService workspaceParticipationQueryService;
	@Autowired
	protected WorkspaceParticipationCommandService workspaceParticipationCommandService;
	@Autowired
	protected WorkspaceReader workspaceReader;
	@Autowired
	protected WorkspaceCommandService workspaceCommandService;
	@Autowired
	protected MemberCommandService memberCommandService;
	@Autowired
	protected MemberQueryService memberQueryService;
	@Autowired
	protected RetryCodeGenerationOnExceptionService workspaceCreateService;
	@Autowired
	protected InvitationCommandService invitationCommandService;
	@Autowired
	protected InvitationQueryService invitationQueryService;
	@Autowired
	protected PositionCommandService positionCommandService;
	@Autowired
	protected PositionQueryService positionQueryService;
	@Autowired
	protected TeamCommandService teamCommandService;
	@Autowired
	protected TeamQueryService teamQueryService;
	@Autowired
	protected IssueCommandService issueCommandService;
	@Autowired
	protected IssueRelationCommandService issueRelationCommandService;
	@Autowired
	protected ReviewCommandService reviewCommandService;
	@Autowired
	protected ReviewerCommandService reviewerCommandService;
	@Autowired
	protected AssigneeCommandService assigneeCommandService;
	@Autowired
	protected IssueCommentCommandService issueCommentCommandService;
	@Autowired
	protected ReviewCommentCommandService reviewCommentCommandService;
	@Autowired
	protected SprintCommandService sprintCommandService;
	@Autowired
	protected SprintQueryService sprintQueryService;
	@Autowired
	protected SprintReader sprintReader;

	/**
	 * Validator
	 */
	@Autowired
	protected MemberValidator memberValidator;
	@Autowired
	protected WorkspaceValidator workspaceValidator;
	@Autowired
	protected CircularDependencyChecker circularDependencyChecker;

	/**
	 * Repository
	 */
	@Autowired
	protected WorkspaceRepository workspaceRepository;
	@Autowired
	protected WorkspaceMemberRepository workspaceMemberRepository;
	@Autowired
	protected MemberRepository memberRepository;
	@Autowired
	protected InvitationRepository invitationRepository;
	@Autowired
	protected PositionRepository positionRepository;
	@Autowired
	protected TeamRepository teamRepository;
	@Autowired
	protected IssueRepository issueRepository;
	@Autowired
	protected ReviewRepository reviewRepository;
	@Autowired
	protected IssueReviewerRepository issueReviewerRepository;
	@Autowired
	protected CommentRepository commentRepository;
	@Autowired
	protected SprintRepository sprintRepository;
	@Autowired
	protected SprintQueryRepository sprintQueryRepository;

	/**
	 * Fixture
	 */
	@Autowired
	protected TestDataFixture testDataFixture;
}
