package com.tissue.support.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.tissue.api.invitation.application.service.command.InvitationCommandService;
import com.tissue.api.invitation.infrastructure.repository.InvitationRepository;
import com.tissue.api.member.application.service.command.MemberCommandService;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.position.application.service.command.PositionCommandService;
import com.tissue.api.position.application.service.command.PositionFinder;
import com.tissue.api.position.infrastructure.repository.PositionRepository;
import com.tissue.api.security.authentication.application.service.AuthenticationService;
import com.tissue.api.workspace.application.service.command.create.WorkspaceCreateRetryOnCodeCollisionService;
import com.tissue.api.workspace.infrastructure.repository.WorkspaceRepository;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberService;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;
import com.tissue.support.fixture.api.LoginApiFixture;
import com.tissue.support.fixture.api.MemberApiFixture;
import com.tissue.support.util.DatabaseCleaner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class RestAssuredTestHelper {
	@LocalServerPort
	protected int port;

	/**
	 * Common
	 */
	@Autowired
	protected DatabaseCleaner databaseCleaner;

	/**
	 * Service
	 */
	@Autowired
	protected AuthenticationService authenticationService;
	@Autowired
	protected WorkspaceMemberService workspaceMemberService;
	@Autowired
	protected WorkspaceCreateRetryOnCodeCollisionService workspaceCreateService;
	@Autowired
	protected InvitationCommandService invitationCommandService;
	@Autowired
	protected MemberCommandService memberCommandService;
	@Autowired
	protected PositionCommandService positionCommandService;
	@Autowired
	protected PositionFinder positionFinder;

	/**
	 * Repository
	 */
	@Autowired
	protected WorkspaceRepository workspaceRepository;
	@Autowired
	protected MemberRepository memberRepository;
	@Autowired
	protected WorkspaceMemberRepository workspaceMemberRepository;
	@Autowired
	protected InvitationRepository invitationRepository;
	@Autowired
	protected PositionRepository positionRepository;

	/**
	 * Fixture
	 */
	@Autowired
	protected LoginApiFixture loginApiFixture;
	@Autowired
	protected MemberApiFixture memberApiFixture;

}
