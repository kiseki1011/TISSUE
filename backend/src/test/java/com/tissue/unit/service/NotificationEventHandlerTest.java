package com.tissue.unit.service;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.tissue.api.event.NotificationEventHandler;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.event.IssueCreatedEvent;
import com.tissue.api.notification.service.command.NotificationCommandService;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {

	@Mock
	private NotificationCommandService notificationService;

	@Mock
	private WorkspaceMemberReader workspaceMemberReader;

	@Mock
	private WorkspaceMemberRepository workspaceMemberRepository;

	@InjectMocks
	private NotificationEventHandler notificationEventHandler;

	@Test
	@DisplayName("이슈 생성 이벤트 발생 시 모든 워크스페이스 멤버에게 알림이 성공적으로 생성되어야 함")
	void handleIssueCreated_WhenAllNotificationsCreated_ShouldLogSuccess() {
		// given
		Issue issue = mock(Issue.class);
		when(issue.getWorkspaceCode()).thenReturn("TESTCODE");
		when(issue.getTitle()).thenReturn("test issue");
		when(issue.getIssueKey()).thenReturn("ISSUE-1");

		// event
		Long actorId = 123L;
		IssueCreatedEvent event = new IssueCreatedEvent(issue, actorId);

		// actor
		WorkspaceMember actor = mock(WorkspaceMember.class);
		when(actor.getNickname()).thenReturn("testuser");

		// create 2 workspace members
		List<WorkspaceMember> members = Arrays.asList(
			mock(WorkspaceMember.class),
			mock(WorkspaceMember.class)
		);

		when(workspaceMemberReader.findWorkspaceMember(actorId)).thenReturn(actor);
		when(workspaceMemberRepository.findAllByWorkspaceCode(anyString())).thenReturn(members);

		// when
		notificationEventHandler.handleIssueCreated(event);

		// then
		verify(notificationService, times(2)).createNotification(
			any(), any(), any(), any(), any(), any(), any(), any(), any()
		);
	}

	@Test
	@DisplayName("일부 알림 생성이 실패해도 다른 멤버에 대한 알림 처리는 계속 진행되어야 한다")
	void handleIssueCreated_WhenNotificationFails_ShouldLogAndContinue() {
		// given
		Issue issue = mock(Issue.class);
		when(issue.getWorkspaceCode()).thenReturn("TESTCODE");
		when(issue.getTitle()).thenReturn("test issue");
		when(issue.getIssueKey()).thenReturn("ISSUE-1");

		// event
		IssueCreatedEvent event = new IssueCreatedEvent(issue, 123L);

		// actor
		WorkspaceMember actor = mock(WorkspaceMember.class);
		when(actor.getNickname()).thenReturn("testuser");

		// create 2 workspace members
		List<WorkspaceMember> members = Arrays.asList(
			mock(WorkspaceMember.class),
			mock(WorkspaceMember.class)
		);

		when(workspaceMemberReader.findWorkspaceMember(anyLong())).thenReturn(actor);
		when(workspaceMemberRepository.findAllByWorkspaceCode(anyString())).thenReturn(members);

		doThrow(new RuntimeException("notification for first member failed")) // throw exception on first call
			.doNothing() // do nothing on second call
			.when(notificationService)
			.createNotification(any(), any(), any(), any(), any(), any(), any(), any(), any());

		// when
		notificationEventHandler.handleIssueCreated(event);

		// then
		verify(notificationService, times(2))
			.createNotification(any(), any(), any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("중복 알림으로 인한 무결성 위반 예외가 발생해도 처리가 중단되지 않아야 함")
	void handleIssueCreated_WhenDuplicateNotification_ShouldHandleViolationException() {
		// given
		Issue issue = mock(Issue.class);
		when(issue.getWorkspaceCode()).thenReturn("TESTCODE");
		when(issue.getTitle()).thenReturn("test issue");
		when(issue.getIssueKey()).thenReturn("ISSUE-1");

		// event
		Long actorId = 123L;
		IssueCreatedEvent event = new IssueCreatedEvent(issue, actorId);

		// actor
		WorkspaceMember actor = mock(WorkspaceMember.class);
		when(actor.getNickname()).thenReturn("testuser");

		// create 2 workspace members
		List<WorkspaceMember> members = Arrays.asList(
			mock(WorkspaceMember.class),
			mock(WorkspaceMember.class)
		);

		when(workspaceMemberReader.findWorkspaceMember(anyLong())).thenReturn(actor);
		when(workspaceMemberRepository.findAllByWorkspaceCode(anyString())).thenReturn(members);

		// throw exception for every notification creation
		doThrow(new DataIntegrityViolationException("Duplicate notification"))
			.when(notificationService).createNotification(
				any(), any(), any(), any(), any(), any(), any(), any(), any()
			);

		// when
		notificationEventHandler.handleIssueCreated(event);

		// then
		verify(notificationService, times(2)).createNotification(
			any(), any(), any(), any(), any(), any(), any(), any(), any()
		);
	}

}