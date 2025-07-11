package com.tissue.support.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.tissue.api.comment.application.service.command.IssueCommentCommandService;
import com.tissue.api.comment.application.service.command.ReviewCommentCommandService;
import com.tissue.api.comment.infrastructure.repository.CommentRepository;
import com.tissue.api.invitation.application.service.command.InvitationCommandService;
import com.tissue.api.invitation.application.service.query.InvitationQueryService;
import com.tissue.api.invitation.application.service.reader.InvitationReader;
import com.tissue.api.invitation.infrastructure.repository.InvitationRepository;
import com.tissue.api.issue.application.service.command.IssueAssigneeCommandService;
import com.tissue.api.issue.application.service.command.IssueCommandService;
import com.tissue.api.issue.application.service.command.IssueRelationCommandService;
import com.tissue.api.issue.application.service.command.IssueReviewerCommandService;
import com.tissue.api.issue.domain.service.validator.CircularDependencyValidator;
import com.tissue.api.issue.infrastructure.repository.IssueRepository;
import com.tissue.api.issue.infrastructure.repository.IssueReviewerRepository;
import com.tissue.api.member.application.service.command.MemberCommandService;
import com.tissue.api.member.application.service.query.MemberQueryService;
import com.tissue.api.member.domain.service.MemberValidator;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.position.application.service.command.PositionCommandService;
import com.tissue.api.position.application.service.command.PositionReader;
import com.tissue.api.position.application.service.query.PositionQueryService;
import com.tissue.api.position.infrastructure.repository.PositionRepository;
import com.tissue.api.review.application.service.command.ReviewCommandService;
import com.tissue.api.review.infrastructure.repository.ReviewRepository;
import com.tissue.api.sprint.application.service.command.SprintCommandService;
import com.tissue.api.sprint.application.service.command.SprintReader;
import com.tissue.api.sprint.application.service.query.SprintQueryService;
import com.tissue.api.sprint.infrastructure.repository.SprintQueryRepository;
import com.tissue.api.sprint.infrastructure.repository.SprintRepository;
import com.tissue.api.team.application.service.command.TeamCommandService;
import com.tissue.api.team.application.service.command.TeamReader;
import com.tissue.api.team.infrastructure.repository.TeamRepository;
import com.tissue.api.util.WorkspaceCodeParser;
import com.tissue.api.workspace.application.service.command.WorkspaceCommandService;
import com.tissue.api.workspace.application.service.command.WorkspaceReader;
import com.tissue.api.workspace.application.service.command.create.WorkspaceCreateRetryOnCodeCollisionService;
import com.tissue.api.workspace.application.service.query.WorkspaceQueryService;
import com.tissue.api.workspace.domain.service.WorkspaceAuthenticationService;
import com.tissue.api.workspace.domain.service.validator.WorkspaceValidator;
import com.tissue.api.workspace.infrastructure.repository.WorkspaceRepository;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberCommandService;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberInviteService;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;
import com.tissue.api.workspacemember.application.service.command.WorkspaceParticipationCommandService;
import com.tissue.api.workspacemember.application.service.query.WorkspaceParticipationQueryService;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;
import com.tissue.support.fixture.TestDataFixture;
import com.tissue.support.util.DatabaseCleaner;

import jakarta.persistence.EntityManager;

@SpringBootTest
@TestPropertySource(properties = {
	"jwt.secret=ThisIsADefaultTestSecretThatIs32Chars"
})
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
	protected WorkspaceQueryService workspaceQueryService;
	@Autowired
	protected WorkspaceCommandService workspaceCommandService;
	@Autowired
	protected MemberCommandService memberCommandService;
	@Autowired
	protected MemberQueryService memberQueryService;
	@Autowired
	protected WorkspaceCreateRetryOnCodeCollisionService workspaceCreateService;
	@Autowired
	protected InvitationCommandService invitationCommandService;
	@Autowired
	protected InvitationQueryService invitationQueryService;
	@Autowired
	protected InvitationReader invitationReader;
	@Autowired
	protected PositionCommandService positionCommandService;
	@Autowired
	protected PositionQueryService positionQueryService;
	@Autowired
	protected PositionReader positionReader;
	@Autowired
	protected TeamCommandService teamCommandService;
	@Autowired
	protected TeamReader teamReader;
	// @Autowired
	// protected TeamQueryService teamQueryService;
	@Autowired
	protected IssueCommandService issueCommandService;
	@Autowired
	protected IssueRelationCommandService issueRelationCommandService;
	@Autowired
	protected ReviewCommandService reviewCommandService;
	@Autowired
	protected IssueReviewerCommandService issueReviewerCommandService;
	@Autowired
	protected IssueAssigneeCommandService issueAssigneeCommandService;
	@Autowired
	protected IssueCommentCommandService issueCommentCommandService;
	@Autowired
	protected ReviewCommentCommandService reviewCommentCommandService;
	@Autowired
	protected SprintCommandService sprintCommandService;
	@Autowired
	protected SprintQueryService sprintQueryService;
	@Autowired
	protected WorkspaceAuthenticationService workspaceAuthenticationService;
	@Autowired
	protected SprintReader sprintReader;
	// @Autowired
	// protected NotificationMessageFactory notificationMessageFactory;

	/**
	 * Validator
	 */
	@Autowired
	protected MemberValidator memberValidator;
	@Autowired
	protected WorkspaceValidator workspaceValidator;
	@Autowired
	protected CircularDependencyValidator circularDependencyValidator;

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
