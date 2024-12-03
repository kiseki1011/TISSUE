package com.uranus.taskmanager.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.uranus.taskmanager.api.invitation.domain.repository.InvitationRepository;
import com.uranus.taskmanager.api.invitation.service.InvitationService;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.service.command.MemberCommandService;
import com.uranus.taskmanager.api.member.service.query.MemberQueryService;
import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.service.command.WorkspaceCommandService;
import com.uranus.taskmanager.api.workspace.service.command.create.RetryCodeGenerationOnExceptionService;
import com.uranus.taskmanager.api.workspace.service.query.WorkspaceQueryService;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.api.workspacemember.service.command.WorkspaceMemberCommandService;
import com.uranus.taskmanager.api.workspacemember.service.command.WorkspaceMemberInviteService;
import com.uranus.taskmanager.api.workspacemember.service.command.WorkspaceParticipationCommandService;
import com.uranus.taskmanager.api.workspacemember.service.query.MemberWorkspaceQueryService;
import com.uranus.taskmanager.api.workspacemember.service.query.WorkspaceMemberQueryService;
import com.uranus.taskmanager.fixture.repository.MemberRepositoryFixture;
import com.uranus.taskmanager.fixture.repository.WorkspaceRepositoryFixture;
import com.uranus.taskmanager.util.DatabaseCleaner;

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
	protected MemberWorkspaceQueryService memberWorkspaceQueryService;
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
	protected InvitationService invitationService;

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

	/**
	 * Fixture
	 */
	@Autowired
	protected WorkspaceRepositoryFixture workspaceRepositoryFixture;
	@Autowired
	protected MemberRepositoryFixture memberRepositoryFixture;

}
