package com.uranus.taskmanager.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.uranus.taskmanager.api.invitation.domain.repository.InvitationRepository;
import com.uranus.taskmanager.api.invitation.service.InvitationService;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.service.MemberQueryService;
import com.uranus.taskmanager.api.member.service.MemberService;
import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.service.WorkspaceCommandService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceQueryService;
import com.uranus.taskmanager.api.workspace.service.create.CheckCodeDuplicationService;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.api.workspacemember.service.WorkspaceMemberService;
import com.uranus.taskmanager.fixture.repository.MemberRepositoryFixture;
import com.uranus.taskmanager.fixture.repository.WorkspaceRepositoryFixture;
import com.uranus.taskmanager.util.DatabaseCleaner;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
	protected WorkspaceMemberService workspaceMemberService;
	@Autowired
	protected WorkspaceQueryService workspaceQueryService;
	@Autowired
	protected WorkspaceCommandService workspaceCommandService;
	@Autowired
	protected MemberService memberService;
	@Autowired
	protected MemberQueryService memberQueryService;
	@Autowired
	protected CheckCodeDuplicationService workspaceCreateService;
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
