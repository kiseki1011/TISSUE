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
import com.tissue.api.issue.adapter.in.web.IssueCommandController;
import com.tissue.api.issue.application.service.IssueCommandService;
import com.tissue.api.issue.domain.port.out.IssueCommandRepository;
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
import com.tissue.api.workspace.adapter.in.web.WorkspaceController;
import com.tissue.api.workspace.adapter.in.web.WorkspaceMemberController;
import com.tissue.api.workspace.application.service.command.WorkspaceCommandService;
import com.tissue.api.workspace.application.service.command.WorkspaceCreateService;
import com.tissue.api.workspace.application.service.command.WorkspaceMemberCommandService;
import com.tissue.api.workspace.application.service.command.WorkspaceParticipationService;
import com.tissue.api.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.api.workspace.application.service.query.WorkspaceMemberQueryService;
import com.tissue.api.workspace.application.service.query.WorkspaceQueryService;
import com.tissue.api.workspace.domain.port.out.WorkspaceMemberCommandRepository;
import com.tissue.api.workspace.domain.port.out.WorkspaceCommandRepository;

import deprecated.com.tissue.support.config.WebMvcTestConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest(
	controllers = {
		AuthenticationController.class,
		InvitationController.class,
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
	protected WorkspaceMemberCommandService workspaceMemberCommandService;
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
	protected MemberRepository memberRepository;
	@MockBean
	protected WorkspaceCommandRepository workspaceCommandRepository;
	@MockBean
	protected WorkspaceMemberCommandRepository workspaceMemberCommandRepository;
	@MockBean
	protected InvitationRepository invitationRepository;
	@MockBean
	protected PositionRepository positionRepository;
	@MockBean
	protected IssueCommandRepository issueCommandRepository;

}
