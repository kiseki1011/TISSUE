package com.tissue.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.tissue.api.invitation.domain.repository.InvitationRepository;
import com.tissue.api.invitation.service.command.InvitationCommandService;
import com.tissue.api.invitation.service.query.InvitationQueryService;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.service.command.IssueCommandService;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.service.command.MemberCommandService;
import com.tissue.api.member.service.query.MemberQueryService;
import com.tissue.api.member.validator.MemberValidator;
import com.tissue.api.position.domain.repository.PositionRepository;
import com.tissue.api.position.service.command.PositionCommandService;
import com.tissue.api.position.service.query.PositionQueryService;
import com.tissue.api.review.domain.repository.ReviewRepository;
import com.tissue.api.review.service.ReviewCommandService;
import com.tissue.api.review.validator.ReviewValidator;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.util.WorkspaceCodeParser;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.service.command.WorkspaceCommandService;
import com.tissue.api.workspace.service.command.create.RetryCodeGenerationOnExceptionService;
import com.tissue.api.workspace.service.query.WorkspaceQueryService;
import com.tissue.api.workspace.validator.WorkspaceValidator;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberCommandService;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberInviteService;
import com.tissue.api.workspacemember.service.command.WorkspaceParticipationCommandService;
import com.tissue.api.workspacemember.service.query.WorkspaceMemberQueryService;
import com.tissue.api.workspacemember.service.query.WorkspaceParticipationQueryService;
import com.tissue.fixture.dto.SignupRequestDtoFixture;
import com.tissue.fixture.repository.InvitationRepositoryFixture;
import com.tissue.fixture.repository.MemberRepositoryFixture;
import com.tissue.fixture.repository.PositionRepositoryFixture;
import com.tissue.fixture.repository.WorkspaceRepositoryFixture;
import com.tissue.fixture.service.IssueFixture;
import com.tissue.fixture.service.MemberFixture;
import com.tissue.fixture.service.WorkspaceFixture;
import com.tissue.util.DatabaseCleaner;

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
	protected WorkspaceMemberQueryService workspaceMemberQueryService;
	@Autowired
	protected WorkspaceParticipationQueryService workspaceParticipationQueryService;
	@Autowired
	protected WorkspaceParticipationCommandService workspaceParticipationCommandService;
	@Autowired
	protected WorkspaceQueryService workspaceQueryService;
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
	protected IssueCommandService issueCommandService;
	@Autowired
	protected ReviewCommandService reviewCommandService;

	/**
	 * Validator
	 */
	@Autowired
	protected MemberValidator memberValidator;
	@Autowired
	protected WorkspaceValidator workspaceValidator;
	@Autowired
	protected ReviewValidator reviewValidator;

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
	protected IssueRepository issueRepository;
	@Autowired
	protected ReviewRepository reviewRepository;

	/**
	 * Fixture
	 */
	@Autowired
	protected WorkspaceRepositoryFixture workspaceRepositoryFixture;
	@Autowired
	protected MemberRepositoryFixture memberRepositoryFixture;
	@Autowired
	protected InvitationRepositoryFixture invitationRepositoryFixture;
	@Autowired
	protected SignupRequestDtoFixture signupRequestDtoFixture;
	@Autowired
	protected PositionRepositoryFixture positionRepositoryFixture;
	@Autowired
	protected IssueFixture issueFixture;
	@Autowired
	protected MemberFixture memberFixture;
	@Autowired
	protected WorkspaceFixture workspaceFixture;

}
