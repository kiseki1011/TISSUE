package deprecated.com.tissue.support.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.api.global.config.webmvc.WebMvcConfig;
import com.tissue.api.invitation.application.service.command.InvitationCommandService;
import com.tissue.api.invitation.application.service.finder.InvitationFinder;
import com.tissue.api.invitation.application.service.query.InvitationQueryService;
import com.tissue.api.invitation.infrastructure.repository.InvitationRepository;
import com.tissue.api.invitation.presentation.controller.command.InvitationController;
import com.tissue.api.issue.application.service.IssueService;
import com.tissue.api.issue.infrastructure.repository.IssueRepository;
import com.tissue.api.issue.presentation.controller.IssueController;
import com.tissue.api.issue.application.service.IssueReviewerService;
import com.tissue.api.issue.infrastructure.repository.IssueReviewerRepository;
import com.tissue.api.issue.presentation.controller.IssueReviewerController;
import com.tissue.api.member.application.service.command.MemberCommandService;
import com.tissue.api.member.application.service.query.MemberQueryService;
import com.tissue.api.member.domain.service.MemberValidator;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.member.presentation.controller.MemberController;
import com.tissue.api.member.presentation.controller.MemberQueryController;
import com.tissue.api.position.application.service.command.PositionCommandService;
import com.tissue.api.position.application.service.command.PositionFinder;
import com.tissue.api.position.application.service.query.PositionQueryService;
import com.tissue.api.position.infrastructure.repository.PositionRepository;
import com.tissue.api.position.presentation.controller.PositionController;
import com.tissue.api.security.SecurityConfig;
import com.tissue.api.security.authentication.application.service.AuthenticationService;
import com.tissue.api.security.authentication.jwt.JwtTokenService;
import com.tissue.api.security.authentication.presentation.controller.AuthenticationController;
import com.tissue.api.util.WorkspaceCodeParser;
import com.tissue.api.workspace.application.service.command.WorkspaceCommandService;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.application.service.command.create.WorkspaceCreateRetryOnCodeCollisionService;
import com.tissue.api.workspace.application.service.query.WorkspaceQueryService;
import com.tissue.api.workspace.domain.service.WorkspaceAuthenticationService;
import com.tissue.api.workspace.infrastructure.repository.WorkspaceRepository;
import com.tissue.api.workspace.presentation.controller.command.WorkspaceController;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberInviteService;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberService;
import com.tissue.api.workspacemember.application.service.command.WorkspaceParticipationService;
import com.tissue.api.workspacemember.application.service.query.WorkspaceParticipationQueryService;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.presentation.controller.command.WorkspaceMemberController;
import com.tissue.api.workspacemember.presentation.controller.command.WorkspaceMembershipController;
import com.tissue.api.workspacemember.presentation.controller.command.WorkspaceParticipationController;

import deprecated.com.tissue.support.config.WebMvcTestConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest(
	controllers = {
		AuthenticationController.class,
		InvitationController.class,
		WorkspaceController.class,
		WorkspaceMembershipController.class,
		WorkspaceParticipationController.class,
		WorkspaceMemberController.class,
		MemberController.class,
		MemberQueryController.class,
		PositionController.class,
		IssueController.class,
		// ReviewController.class,
		IssueReviewerController.class
	},
	excludeAutoConfiguration = SecurityAutoConfiguration.class,
	excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
			WebMvcConfig.class,
			HandlerMethodArgumentResolver.class,
			HandlerInterceptor.class,
			SecurityConfig.class
		})
	}
)
@TestPropertySource(properties = {
	"jwt.secret=ThisIsADefaultTestSecretThatIs32Chars"
})
@Import(value = {WebMvcTestConfig.class})
public abstract class ControllerTestHelper {

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected ObjectMapper objectMapper;
	@Autowired
	protected MessageSource messageSource;

	@MockBean
	protected WorkspaceCodeParser workspaceCodeParser;

	/**
	 * Spring Security
	 */
	@MockBean
	protected JwtTokenService jwtTokenService;
	// @MockBean
	// protected JwtAuthenticationFilter jwtAuthenticationFilter;
	// @MockBean
	// protected ExceptionHandlerFilter exceptionHandlerFilter;
	// @MockBean
	// protected JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	// @MockBean
	// protected ApiAccessDeniedHandler apiAccessDeniedHandler;

	/**
	 * Service
	 */
	@MockBean
	protected MemberCommandService memberCommandService;
	@MockBean
	protected MemberQueryService memberQueryService;
	@MockBean
	protected WorkspaceMemberService workspaceMemberService;
	@MockBean
	protected WorkspaceMemberInviteService workspaceMemberInviteService;
	@MockBean
	protected WorkspaceParticipationQueryService workspaceParticipationQueryService;
	@MockBean
	protected WorkspaceParticipationService workspaceParticipationService;
	@MockBean
	protected WorkspaceCreateRetryOnCodeCollisionService workspaceCreateService;
	@MockBean
	protected WorkspaceFinder workspaceFinder;
	@MockBean
	protected WorkspaceQueryService workspaceQueryService;
	@MockBean
	protected WorkspaceCommandService workspaceCommandService;
	@MockBean
	protected AuthenticationService authenticationService;
	@MockBean
	protected InvitationCommandService invitationCommandService;
	@MockBean
	protected InvitationQueryService invitationQueryService;
	@MockBean
	protected InvitationFinder invitationFinder;
	@MockBean
	protected PositionCommandService positionCommandService;
	@MockBean
	protected PositionFinder positionFinder;
	@MockBean
	protected PositionQueryService positionQueryService;
	@MockBean
	protected IssueService issueService;
	// @MockBean
	// protected ReviewCommandService reviewCommandService;
	@MockBean
	protected IssueReviewerService issueReviewerService;
	@MockBean
	protected WorkspaceAuthenticationService workspaceAuthenticationService;

	/**
	 * Validator
	 */
	@MockBean
	protected MemberValidator memberValidator;

	/**
	 * Repository
	 */
	@MockBean
	protected MemberRepository memberRepository;
	@MockBean
	protected WorkspaceRepository workspaceRepository;
	@MockBean
	protected WorkspaceMemberRepository workspaceMemberRepository;
	@MockBean
	protected InvitationRepository invitationRepository;
	@MockBean
	protected PositionRepository positionRepository;
	@MockBean
	protected IssueRepository issueRepository;
	@MockBean
	protected IssueReviewerRepository issueReviewerRepository;

}
