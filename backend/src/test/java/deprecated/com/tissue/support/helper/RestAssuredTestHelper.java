package deprecated.com.tissue.support.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.tissue.member.application.port.out.MemberRepository;
import com.tissue.member.application.service.MemberCommandService;
import com.tissue.position.application.port.out.PositionCommandRepository;
import com.tissue.position.application.service.PositionService;
import com.tissue.position.application.service.finder.PositionFinder;
import com.tissue.security.authentication.application.service.AuthenticationService;
import com.tissue.workspace.application.port.out.WorkspaceCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.workspace.application.service.command.WorkspaceCreateService;
import com.tissue.workspace.application.service.command.WorkspaceMemberManageService;

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
	protected WorkspaceMemberManageService workspaceMemberCommandService;
	@Autowired
	protected WorkspaceCreateService workspaceCreateService;
	@Autowired
	protected MemberCommandService memberCommandService;
	@Autowired
	protected PositionService positionService;
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
	protected PositionCommandRepository positionCommandRepository;

	/**
	 * Fixture
	 */
	@Autowired
	protected LoginApiFixture loginApiFixture;
	@Autowired
	protected MemberApiFixture memberApiFixture;

}
