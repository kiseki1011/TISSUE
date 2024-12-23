package com.tissue.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.position.domain.repository.PositionRepository;
import com.tissue.api.position.service.command.PositionCommandService;
import com.tissue.api.position.service.query.PositionQueryService;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.workspace.service.command.WorkspaceCommandService;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberInviteService;
import com.tissue.api.workspacemember.service.command.WorkspaceParticipationCommandService;
import com.tissue.api.invitation.domain.repository.InvitationRepository;
import com.tissue.api.invitation.service.command.InvitationCommandService;
import com.tissue.api.invitation.service.query.InvitationQueryService;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.service.command.IssueCommandService;
import com.tissue.api.member.service.command.MemberCommandService;
import com.tissue.api.member.service.query.MemberQueryService;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.service.command.create.RetryCodeGenerationOnExceptionService;
import com.tissue.api.workspace.service.query.WorkspaceQueryService;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberCommandService;
import com.tissue.api.workspacemember.service.query.WorkspaceMemberQueryService;
import com.tissue.api.workspacemember.service.query.WorkspaceParticipationQueryService;
import com.tissue.fixture.dto.SignupRequestDtoFixture;
import com.tissue.fixture.repository.InvitationRepositoryFixture;
import com.tissue.fixture.repository.MemberRepositoryFixture;
import com.tissue.fixture.repository.PositionRepositoryFixture;
import com.tissue.fixture.repository.WorkspaceRepositoryFixture;
import com.tissue.util.DatabaseCleaner;

import jakarta.persistence.EntityManager;

@SpringBootTest
public abstract class ServiceIntegrationTestHelper {

	/**
	 * Common
	 */
	@Autowired
	protected DatabaseCleaner databaseCleaner;
	@Autowired
	protected PasswordEncoder passwordEncoder;
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

}
