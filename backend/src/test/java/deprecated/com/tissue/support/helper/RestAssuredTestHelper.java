package deprecated.com.tissue.support.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.tissue.api.member.application.service.command.MemberCommandService;
import com.tissue.api.member.infrastructure.repository.MemberRepository;
import com.tissue.api.position.application.service.command.PositionCommandService;
import com.tissue.api.position.application.service.command.PositionFinder;
import com.tissue.api.position.infrastructure.repository.PositionRepository;
import com.tissue.api.security.authentication.application.service.AuthenticationService;
import com.tissue.api.workspace.application.port.out.WorkspaceCommandRepository;
import com.tissue.api.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.api.workspace.application.service.command.WorkspaceCreateService;
import com.tissue.api.workspace.application.service.command.WorkspaceMemberCommandService;

import deprecated.com.tissue.support.fixture.api.LoginApiFixture;
import deprecated.com.tissue.support.fixture.api.MemberApiFixture;
import deprecated.com.tissue.support.util.DatabaseCleaner;
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
	protected WorkspaceCreateService workspaceCreateService;
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
	protected WorkspaceCommandRepository workspaceCommandRepository;
	@Autowired
	protected MemberRepository memberRepository;
	@Autowired
	protected WorkspaceMemberCommandRepository workspaceMemberCommandRepository;
	// @Autowired
	// protected InvitationRepository invitationRepository;
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
