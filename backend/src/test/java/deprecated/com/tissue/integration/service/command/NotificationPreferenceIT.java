package deprecated.com.tissue.integration.service.command;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
public class NotificationPreferenceIT {

	// @Autowired
	// private NotificationPreferenceService preferenceService;
	//
	// @Autowired
	// private NotificationCommandService notificationCommandService;
	//
	// @Autowired
	// private NotificationPreferenceRepository preferenceRepository;
	//
	// @Autowired
	// private SimpleNotificationMessageFactory simpleNotificationMessageFactory;
	//
	// @Autowired
	// protected TestDataFixture testDataFixture;
	//
	// @Autowired
	// protected DatabaseCleaner databaseCleaner;
	//
	// private NotificationProcessor notificationProcessor;
	//
	// private NotificationSender mockEmailSender;
	//
	// Workspace workspace;
	// Member member;
	// WorkspaceMember workspaceMember;
	//
	// @BeforeEach
	// void setUp() {
	// 	// create workspace
	// 	workspace = testDataFixture.createWorkspace(
	// 		"test workspace",
	// 		null,
	// 		null
	// 	);
	//
	// 	// create member
	// 	member = testDataFixture.createMember("member");
	//
	// 	// add workspace member
	// 	workspaceMember = testDataFixture.createWorkspaceMember(
	// 		member,
	// 		workspace,
	// 		WorkspaceRole.MEMBER
	// 	);
	//
	// 	mockEmailSender = mock(NotificationSender.class);
	// 	when(mockEmailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
	//
	// 	// 실제 notificationProcessor에 직접 mockEmailSender를 주입함
	// 	notificationProcessor = new NotificationProcessor(
	// 		List.of(mockEmailSender),
	// 		preferenceRepository
	// 	);
	// }
	//
	// @AfterEach
	// public void tearDown() {
	// 	databaseCleaner.execute();
	// }
	//
	// @Test
	// @Transactional
	// void shouldNotSendEmail_WhenEmailPreferenceIsDisabled() {
	// 	// given
	// 	NotificationType type = NotificationType.ISSUE_CREATED;
	//
	// 	// 알림 끄기
	// 	preferenceService.updatePreference(workspace.getKey(), member.getId(),
	// 		new UpdateNotificationPreferenceRequest(type, false));
	//
	// 	NotificationMessage message = new NotificationMessage("test", "test");
	//
	// 	// when
	// 	Notification notification = notificationCommandService.createNotification(
	// 		new DummyEvent(member.getId(), workspace.getKey(), type),
	// 		member.getId(),
	// 		message
	// 	);
	//
	// 	// when
	// 	notificationProcessor.process(notification);
	//
	// 	// then
	// 	verify(mockEmailSender, never()).send(any());
	// }
}
