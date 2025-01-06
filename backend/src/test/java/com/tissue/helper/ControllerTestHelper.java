package com.tissue.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.api.global.config.WebMvcConfig;
import com.tissue.api.invitation.domain.repository.InvitationRepository;
import com.tissue.api.invitation.presentation.controller.InvitationController;
import com.tissue.api.invitation.service.command.InvitationCommandService;
import com.tissue.api.invitation.service.query.InvitationQueryService;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.presentation.controller.IssueController;
import com.tissue.api.issue.service.command.IssueCommandService;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.presentation.controller.MemberController;
import com.tissue.api.member.service.command.MemberCommandService;
import com.tissue.api.member.service.query.MemberQueryService;
import com.tissue.api.member.validator.MemberValidator;
import com.tissue.api.position.domain.repository.PositionRepository;
import com.tissue.api.position.presentation.controller.PositionController;
import com.tissue.api.position.service.command.PositionCommandService;
import com.tissue.api.position.service.query.PositionQueryService;
import com.tissue.api.review.presentation.controller.ReviewController;
import com.tissue.api.review.service.ReviewCommandService;
import com.tissue.api.security.authentication.presentation.controller.AuthenticationController;
import com.tissue.api.security.authentication.service.AuthenticationService;
import com.tissue.api.security.session.SessionManager;
import com.tissue.api.security.session.SessionValidator;
import com.tissue.api.util.WorkspaceCodeParser;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.presentation.controller.WorkspaceController;
import com.tissue.api.workspace.service.command.WorkspaceCommandService;
import com.tissue.api.workspace.service.command.create.CheckCodeDuplicationService;
import com.tissue.api.workspace.service.query.WorkspaceQueryService;
import com.tissue.api.workspace.validator.WorkspaceValidator;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.presentation.controller.WorkspaceMemberInfoController;
import com.tissue.api.workspacemember.presentation.controller.WorkspaceMembershipController;
import com.tissue.api.workspacemember.presentation.controller.WorkspaceParticipationController;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberCommandService;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberInviteService;
import com.tissue.api.workspacemember.service.command.WorkspaceParticipationCommandService;
import com.tissue.api.workspacemember.service.query.WorkspaceParticipationQueryService;
import com.tissue.config.WebMvcTestConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest(
	controllers = {
		AuthenticationController.class,
		InvitationController.class,
		WorkspaceController.class,
		WorkspaceMembershipController.class,
		WorkspaceParticipationController.class,
		WorkspaceMemberInfoController.class,
		MemberController.class,
		PositionController.class,
		IssueController.class,
		ReviewController.class
	},
	excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
			WebMvcConfig.class,
			HandlerMethodArgumentResolver.class,
			HandlerInterceptor.class
		})
	}
)
@Import(value = WebMvcTestConfig.class)
public abstract class ControllerTestHelper {

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected ObjectMapper objectMapper;

	@MockBean
	protected WorkspaceCodeParser workspaceCodeParser;

	/**
	 * Session
	 */
	@MockBean
	protected SessionManager sessionManager;
	@MockBean
	protected SessionValidator sessionValidator;

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
	protected WorkspaceMemberInviteService workspaceMemberInviteService;
	@MockBean
	protected WorkspaceParticipationQueryService workspaceParticipationQueryService;
	@MockBean
	protected WorkspaceParticipationCommandService workspaceParticipationCommandService;
	@MockBean
	protected CheckCodeDuplicationService workspaceCreateService;
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
	protected PositionCommandService positionCommandService;
	@MockBean
	protected PositionQueryService positionQueryService;
	@MockBean
	protected IssueCommandService issueCommandService;
	@MockBean
	protected ReviewCommandService reviewCommandService;

	/**
	 * Validator
	 */
	@MockBean
	protected MemberValidator memberValidator;
	@MockBean
	protected WorkspaceValidator workspaceValidator;

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

}
