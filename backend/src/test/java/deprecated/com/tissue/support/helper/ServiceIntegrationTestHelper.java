package deprecated.com.tissue.support.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.tissue.comment.application.service.command.IssueCommentCommandService;
import com.tissue.comment.application.service.command.ReviewCommentCommandService;
import com.tissue.comment.infrastructure.repository.CommentRepository;
import com.tissue.issue.application.port.out.IssueCommandRepository;
import com.tissue.issue.application.service.IssueCommandService;
import com.tissue.issue.application.service.IssueParticipantService;
import com.tissue.issue.application.service.IssueRelationService;
import com.tissue.member.application.port.out.MemberRepository;
import com.tissue.member.application.service.MemberCommandService;
import com.tissue.member.application.service.MemberQueryService;
import com.tissue.member.application.service.validator.MemberValidator;
import com.tissue.position.application.port.out.PositionCommandRepository;
import com.tissue.position.application.service.PositionService;
import com.tissue.position.application.service.finder.PositionFinder;
import com.tissue.security.authentication.application.service.AuthenticationService;
import com.tissue.security.authentication.jwt.JwtTokenService;
import com.tissue.sprint.application.port.out.SprintQueryRepository;
import com.tissue.sprint.application.service.SprintCommandService;
import com.tissue.sprint.application.service.SprintQueryService;
import com.tissue.sprint.application.service.finder.SprintFinder;
import com.tissue.team.application.service.command.TeamCommandService;
import com.tissue.team.application.service.command.TeamFinder;
import com.tissue.team.infrastructure.repository.TeamRepository;
import com.tissue.workspace.application.port.out.WorkspaceCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.workspace.application.service.command.WorkspaceCommandService;
import com.tissue.workspace.application.service.command.WorkspaceCreateService;
import com.tissue.workspace.application.service.command.WorkspaceMemberManageService;
import com.tissue.workspace.application.service.command.WorkspaceParticipationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.application.service.query.WorkspaceMemberQueryService;
import com.tissue.workspace.application.service.query.WorkspaceQueryService;

import deprecated.com.tissue.support.fixture.TestDataFixture;
import deprecated.com.tissue.support.util.DatabaseCleaner;
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
	protected WorkspaceMemberManageService workspaceMemberCommandService;
	@Autowired
	protected WorkspaceMemberFinder workspaceMemberFinder;
	@Autowired
	protected WorkspaceMemberQueryService workspaceMemberQueryService;
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
	protected WorkspaceCreateService workspaceCreateService;
	@Autowired
	protected PositionService positionService;
	@Autowired
	protected PositionFinder positionFinder;
	@Autowired
	protected TeamCommandService teamCommandService;
	@Autowired
	protected TeamFinder teamFinder;
	@Autowired
	protected IssueCommandService issueCommandService;
	@Autowired
	protected IssueRelationService issueRelationService;
	@Autowired
	protected IssueParticipantService issueParticipantService;
	@Autowired
	protected IssueCommentCommandService issueCommentCommandService;
	@Autowired
	protected ReviewCommentCommandService reviewCommentCommandService;
	@Autowired
	protected SprintCommandService sprintCommandService;
	@Autowired
	protected SprintQueryService sprintQueryService;
	@Autowired
	protected SprintFinder sprintFinder;

	/**
	 * Validator
	 */
	@Autowired
	protected MemberValidator memberValidator;

	/**
	 * Repository
	 */
	@Autowired
	protected WorkspaceCommandRepository workspaceCommandRepository;
	@Autowired
	protected WorkspaceMemberCommandRepository workspaceMemberCommandRepository;
	@Autowired
	protected MemberRepository memberRepository;
	// @Autowired
	// protected InvitationRepository invitationRepository;
	@Autowired
	protected PositionCommandRepository positionCommandRepository;
	@Autowired
	protected TeamRepository teamRepository;
	@Autowired
	protected IssueCommandRepository issueCommandRepository;
	@Autowired
	protected CommentRepository commentRepository;
	@Autowired
	protected SprintQueryRepository sprintQueryRepository;

	/**
	 * Fixture
	 */
	@Autowired
	protected TestDataFixture testDataFixture;
}
