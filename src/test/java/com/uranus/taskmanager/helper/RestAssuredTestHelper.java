package com.uranus.taskmanager.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.uranus.taskmanager.api.authentication.service.AuthenticationService;
import com.uranus.taskmanager.api.invitation.repository.InvitationRepository;
import com.uranus.taskmanager.api.invitation.service.InvitationService;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.member.service.MemberService;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.service.CheckCodeDuplicationService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceAccessService;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.fixture.api.LoginApiFixture;
import com.uranus.taskmanager.fixture.api.MemberApiFixture;
import com.uranus.taskmanager.util.DatabaseCleaner;

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
	protected WorkspaceAccessService workspaceAccessService;
	@Autowired
	protected CheckCodeDuplicationService workspaceCreateService;
	@Autowired
	protected InvitationService invitationService;
	@Autowired
	protected MemberService memberService;

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

	/**
	 * Fixture
	 */
	@Autowired
	protected LoginApiFixture loginApiFixture;
	@Autowired
	protected MemberApiFixture memberApiFixture;

}
