package com.tissue.integration.service.command;

import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.notification.application.service.command.NotificationCommandService;
import com.tissue.api.notification.application.service.command.NotificationPreferenceService;
import com.tissue.api.notification.application.service.command.NotificationProcessor;
import com.tissue.api.notification.domain.enums.NotificationChannel;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.model.Notification;
import com.tissue.api.notification.domain.model.vo.NotificationMessage;
import com.tissue.api.notification.domain.service.sender.NotificationSender;
import com.tissue.api.notification.infrastructure.message.SimpleNotificationMessageFactory;
import com.tissue.api.notification.infrastructure.repository.NotificationPreferenceRepository;
import com.tissue.api.notification.presentation.dto.request.UpdateNotificationPreferenceRequest;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
import com.tissue.support.dummy.DummyEvent;
import com.tissue.support.fixture.TestDataFixture;
import com.tissue.support.util.DatabaseCleaner;

@SpringBootTest
@AutoConfigureMockMvc
public class NotificationPreferenceIT {

	@Autowired
	private NotificationPreferenceService preferenceService;

	@Autowired
	private NotificationCommandService notificationCommandService;

	@Autowired
	private NotificationPreferenceRepository preferenceRepository;

	@Autowired
	private SimpleNotificationMessageFactory simpleNotificationMessageFactory;

	@Autowired
	protected TestDataFixture testDataFixture;

	@Autowired
	protected DatabaseCleaner databaseCleaner;

	private NotificationProcessor notificationProcessor;

	private NotificationSender mockEmailSender;

	Workspace workspace;
	Member member;
	WorkspaceMember workspaceMember;

	@BeforeEach
	void setUp() {
		// create workspace
		workspace = testDataFixture.createWorkspace(
			"test workspace",
			null,
			null
		);

		// create member
		member = testDataFixture.createMember("member");

		// add workspace member
		workspaceMember = testDataFixture.createWorkspaceMember(
			member,
			workspace,
			WorkspaceRole.MEMBER
		);

		mockEmailSender = mock(NotificationSender.class);
		when(mockEmailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);

		// 실제 notificationProcessor에 직접 mockEmailSender를 주입함
		notificationProcessor = new NotificationProcessor(
			List.of(mockEmailSender),
			preferenceRepository
		);
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@Transactional
	void shouldNotSendEmail_WhenEmailPreferenceIsDisabled() {
		// given
		NotificationType type = NotificationType.ISSUE_CREATED;

		// 알림 끄기
		preferenceService.updatePreference(workspace.getCode(), member.getId(),
			new UpdateNotificationPreferenceRequest(type, false));

		NotificationMessage message = new NotificationMessage("test", "test");

		// when
		Notification notification = notificationCommandService.createNotification(
			new DummyEvent(member.getId(), workspace.getCode(), type),
			member.getId(),
			message
		);

		// when
		notificationProcessor.process(notification);

		// then
		verify(mockEmailSender, never()).send(any());
	}
}
