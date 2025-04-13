package com.tissue.support.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.tissue.api.invitation.domain.repository.InvitationRepository;
import com.tissue.api.invitation.service.command.InvitationCommandService;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.service.command.MemberCommandService;
import com.tissue.api.position.domain.repository.PositionRepository;
import com.tissue.api.position.service.command.PositionCommandService;
import com.tissue.api.position.service.command.PositionReader;
import com.tissue.api.security.authentication.service.AuthenticationService;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.service.command.create.CheckCodeDuplicationService;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberCommandService;
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
	protected WorkspaceMemberCommandService workspaceMemberCommandService;
	@Autowired
	protected CheckCodeDuplicationService workspaceCreateService;
	@Autowired
	protected InvitationCommandService invitationCommandService;
	@Autowired
	protected MemberCommandService memberCommandService;
	@Autowired
	protected PositionCommandService positionCommandService;
	@Autowired
	protected PositionReader positionReader;

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
