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
import com.tissue.api.invitation.application.service.finder.InvitationFinder;
import com.tissue.api.invitation.application.service.query.InvitationQueryService;
import com.tissue.api.invitation.infrastructure.repository.InvitationRepository;
import com.tissue.api.issue.base.application.service.IssueRelationService;
import com.tissue.api.issue.base.application.service.IssueService;
import com.tissue.api.issue.base.domain.service.CircularDependencyValidator;
import com.tissue.api.issue.base.infrastructure.repository.IssueRepository;
import com.tissue.api.issue.collaborator.application.service.IssueCollaboratorService;
import com.tissue.api.issue.collaborator.application.service.IssueReviewerService;
import com.tissue.api.issue.collaborator.infrastructure.repository.IssueReviewerRepository;
import com.tissue.api.member.application.service.command.MemberCommandService;
import com.tissue.api.member.application.service.query.MemberQueryService;
import com.tissue.api.member.domain.service.MemberValidator;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.position.application.service.command.PositionCommandService;
import com.tissue.api.position.application.service.command.PositionFinder;
import com.tissue.api.position.application.service.query.PositionQueryService;
import com.tissue.api.position.infrastructure.repository.PositionRepository;
import com.tissue.api.security.authentication.application.service.AuthenticationService;
import com.tissue.api.security.authentication.jwt.JwtTokenService;
import com.tissue.api.sprint.application.service.command.SprintCommandService;
import com.tissue.api.sprint.application.service.command.SprintFinder;
import com.tissue.api.sprint.application.service.query.SprintQueryService;
import com.tissue.api.sprint.infrastructure.repository.SprintQueryRepository;
import com.tissue.api.sprint.infrastructure.repository.SprintRepository;
import com.tissue.api.team.application.service.command.TeamCommandService;
import com.tissue.api.team.application.service.command.TeamFinder;
import com.tissue.api.team.infrastructure.repository.TeamRepository;
import com.tissue.api.util.WorkspaceCodeParser;
import com.tissue.api.workspace.application.service.command.WorkspaceCommandService;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.application.service.command.create.WorkspaceCreateRetryOnCodeCollisionService;
import com.tissue.api.workspace.application.service.query.WorkspaceQueryService;
import com.tissue.api.workspace.domain.service.WorkspaceAuthenticationService;
import com.tissue.api.workspace.infrastructure.repository.WorkspaceRepository;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberFinder;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberInviteService;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberService;
import com.tissue.api.workspacemember.application.service.command.WorkspaceParticipationService;
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
	 * Security
	 */
	@Autowired
	protected JwtTokenService jwtTokenService;

	/**
	 * Service
	 */
	@Autowired
	protected AuthenticationService authenticationService;
	@Autowired
	protected WorkspaceMemberService workspaceMemberService;
	@Autowired
	protected WorkspaceMemberInviteService workspaceMemberInviteService;
	@Autowired
	protected WorkspaceMemberFinder workspaceMemberFinder;
	@Autowired
	protected WorkspaceParticipationQueryService workspaceParticipationQueryService;
	@Autowired
	protected WorkspaceParticipationService workspaceParticipationService;
	@Autowired
	protected WorkspaceFinder workspaceFinder;
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
	protected InvitationFinder invitationFinder;
	@Autowired
	protected PositionCommandService positionCommandService;
	@Autowired
	protected PositionQueryService positionQueryService;
	@Autowired
	protected PositionFinder positionFinder;
	@Autowired
	protected TeamCommandService teamCommandService;
	@Autowired
	protected TeamFinder teamFinder;
	@Autowired
	protected IssueService issueService;
	@Autowired
	protected IssueRelationService issueRelationService;
	// @Autowired
	// protected ReviewCommandService reviewCommandService;
	@Autowired
	protected IssueReviewerService issueReviewerService;
	@Autowired
	protected IssueCollaboratorService issueCollaboratorService;
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
	protected SprintFinder sprintFinder;

	/**
	 * Validator
	 */
	@Autowired
	protected MemberValidator memberValidator;
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
	// @Autowired
	// protected ReviewRepository reviewRepository;
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
