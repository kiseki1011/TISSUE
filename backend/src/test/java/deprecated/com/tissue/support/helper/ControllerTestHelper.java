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
import com.tissue.global.config.webmvc.WebMvcConfig;
import com.tissue.issue.adapter.in.web.IssueCommandController;
import com.tissue.issue.application.port.out.IssueCommandRepository;
import com.tissue.issue.application.service.IssueCommandService;
import com.tissue.member.adapter.in.web.MemberController;
import com.tissue.member.adapter.in.web.MemberQueryController;
import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.application.service.MemberCommandService;
import com.tissue.member.application.service.MemberQueryService;
import com.tissue.member.application.service.validator.MemberValidator;
import com.tissue.position.adapter.in.web.PositionController;
import com.tissue.position.application.port.out.PositionCommandRepository;
import com.tissue.position.application.service.PositionService;
import com.tissue.position.application.service.finder.PositionFinder;
import com.tissue.security.SecurityConfig;
import com.tissue.security.authentication.application.service.AuthenticationService;
import com.tissue.security.authentication.jwt.JwtTokenService;
import com.tissue.security.authentication.presentation.controller.AuthenticationController;
import com.tissue.workspace.adapter.in.web.WorkspaceController;
import com.tissue.workspace.adapter.in.web.WorkspaceMemberController;
import com.tissue.workspace.application.port.out.WorkspaceCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.workspace.application.service.command.WorkspaceCommandService;
import com.tissue.workspace.application.service.command.WorkspaceCreateService;
import com.tissue.workspace.application.service.command.WorkspaceMemberManageService;
import com.tissue.workspace.application.service.command.WorkspaceParticipationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.application.service.query.WorkspaceMemberQueryService;
import com.tissue.workspace.application.service.query.WorkspaceQueryService;

import deprecated.com.tissue.support.config.WebMvcTestConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest(
	controllers = {
		AuthenticationController.class,
		WorkspaceController.class,
		WorkspaceMemberController.class,
		MemberController.class,
		MemberQueryController.class,
		PositionController.class,
		IssueCommandController.class,
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

	/**
	 * Spring Security
	 */
	@MockBean
	protected JwtTokenService jwtTokenService;

	/**
	 * Service
	 */
	@MockBean
	protected MemberCommandService memberCommandService;
	@MockBean
	protected MemberQueryService memberQueryService;
	@MockBean
	protected WorkspaceMemberManageService workspaceMemberCommandService;
	@MockBean
	protected WorkspaceMemberQueryService workspaceMemberQueryService;
	@MockBean
	protected WorkspaceParticipationService workspaceParticipationService;
	@MockBean
	protected WorkspaceCreateService workspaceCreateService;
	@MockBean
	protected WorkspaceFinder workspaceFinder;
	@MockBean
	protected WorkspaceQueryService workspaceQueryService;
	@MockBean
	protected WorkspaceCommandService workspaceCommandService;
	@MockBean
	protected AuthenticationService authenticationService;
	@MockBean
	protected PositionService positionService;
	@MockBean
	protected PositionFinder positionFinder;
	@MockBean
	protected IssueCommandService issueCommandService;

	/**
	 * Validator
	 */
	@MockBean
	protected MemberValidator memberValidator;

	/**
	 * Repository
	 */
	@MockBean
	protected MemberCommandRepository memberCommandRepository;
	@MockBean
	protected WorkspaceCommandRepository workspaceCommandRepository;
	@MockBean
	protected WorkspaceMemberCommandRepository workspaceMemberCommandRepository;
	// @MockBean
	// protected InvitationRepository invitationRepository;
	@MockBean
	protected PositionCommandRepository positionCommandRepository;
	@MockBean
	protected IssueCommandRepository issueCommandRepository;

}
