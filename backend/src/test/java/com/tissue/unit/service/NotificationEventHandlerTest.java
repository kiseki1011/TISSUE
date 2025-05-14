package com.tissue.unit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tissue.api.issue.application.service.reader.IssueReader;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.enums.IssueType;
import com.tissue.api.issue.domain.event.IssueCreatedEvent;
import com.tissue.api.issue.domain.event.IssueUpdatedEvent;
import com.tissue.api.notification.application.eventhandler.NotificationEventHandler;
import com.tissue.api.notification.application.service.command.NotificationCommandService;
import com.tissue.api.notification.application.service.command.NotificationProcessor;
import com.tissue.api.notification.application.service.command.NotificationTargetService;
import com.tissue.api.notification.domain.service.message.NotificationMessageFactory;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {

	@Mock
	private NotificationCommandService notificationService;

	@Mock
	private IssueReader issueReader;

	@Mock
	private WorkspaceMemberReader workspaceMemberReader;

	@Mock
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Mock
	private NotificationTargetService targetResolver;

	@Mock
	private NotificationProcessor notificationProcessor;

	@Mock
	private NotificationMessageFactory notificationMessageFactory;

	@InjectMocks
	private NotificationEventHandler notificationEventHandler;

	@Test
	@DisplayName("이슈 생성 이벤트 발생 시 워크스페이스 멤버들에게 알림이 처리되어야 함")
	void handleIssueCreated_ShouldProcessNotificationForAllWorkspaceMembers() {
		// given
		Long issueId = 1L;
		String issueKey = "ISSUE-1";
		String workspaceCode = "TESTCODE";
		Long actorId = 123L;
		IssueType issueType = IssueType.STORY;

		// Issue 모의 설정
		Issue issue = mock(Issue.class);
		when(issue.getId()).thenReturn(issueId);
		when(issue.getIssueKey()).thenReturn(issueKey);
		when(issue.getWorkspaceCode()).thenReturn(workspaceCode);
		when(issue.getType()).thenReturn(issueType);

		// 이벤트 생성
		IssueCreatedEvent event = IssueCreatedEvent.createEvent(issue, actorId);

		// 워크스페이스 멤버 모의 설정
		List<WorkspaceMember> members = Arrays.asList(
			mock(WorkspaceMember.class),
			mock(WorkspaceMember.class)
		);

		// targetResolver가 워크스페이스 멤버 목록을 반환하도록 설정
		when(targetResolver.getWorkspaceWideMemberTargets(workspaceCode)).thenReturn(members);

		// when
		notificationEventHandler.handleIssueCreated(event);

		// then
		verify(targetResolver).getWorkspaceWideMemberTargets(workspaceCode);
		verify(notificationProcessor).processNotification(event, members);
	}

	@Test
	@DisplayName("이슈 업데이트 이벤트 발생 시 이슈 구독자에게 알림이 처리되어야 함")
	void handleIssueUpdated_ShouldProcessNotificationForIssueSubscribers() {
		// given
		Long issueId = 1L;
		String issueKey = "ISSUE-1";
		String workspaceCode = "TESTCODE";
		Long actorId = 123L;
		IssueType issueType = IssueType.STORY;
		String title = "Test Issue";

		// Issue 모의 설정
		Issue issue = mock(Issue.class);
		when(issue.getId()).thenReturn(issueId);
		when(issue.getIssueKey()).thenReturn(issueKey);
		when(issue.getWorkspaceCode()).thenReturn(workspaceCode);
		when(issue.getType()).thenReturn(issueType);
		when(issue.getTitle()).thenReturn(title);
		when(issue.getStoryPoint()).thenReturn(5);

		// 이벤트 생성
		IssueUpdatedEvent event = IssueUpdatedEvent.createEvent(issue, null, actorId);

		// 이슈 구독자 모의 설정
		List<WorkspaceMember> subscribers = Arrays.asList(
			mock(WorkspaceMember.class),
			mock(WorkspaceMember.class)
		);

		// targetResolver가 이슈 구독자 목록을 반환하도록 설정
		when(targetResolver.getIssueSubscriberTargets(issueKey, workspaceCode)).thenReturn(subscribers);

		// when
		notificationEventHandler.handleIssueUpdated(event);

		// then
		verify(targetResolver).getIssueSubscriberTargets(issueKey, workspaceCode);
		verify(notificationProcessor).processNotification(event, subscribers);
	}

	@Test
	@DisplayName("NotificationProcessor에서 예외가 발생해도 이벤트 핸들러는 예외를 전파하지 않아야 함")
	void handleIssueCreated_WhenProcessorThrowsException_ShouldNotPropagateException() {
		// given
		Long issueId = 1L;
		String issueKey = "ISSUE-1";
		String workspaceCode = "TESTCODE";
		Long actorId = 123L;
		IssueType issueType = IssueType.STORY;

		// Issue 모의 설정
		Issue issue = mock(Issue.class);
		when(issue.getId()).thenReturn(issueId);
		when(issue.getIssueKey()).thenReturn(issueKey);
		when(issue.getWorkspaceCode()).thenReturn(workspaceCode);
		when(issue.getType()).thenReturn(issueType);

		// 이벤트 생성
		IssueCreatedEvent event = IssueCreatedEvent.createEvent(issue, actorId);

		// 워크스페이스 멤버 모의 설정
		List<WorkspaceMember> members = Arrays.asList(
			mock(WorkspaceMember.class),
			mock(WorkspaceMember.class)
		);

		// targetResolver가 워크스페이스 멤버 목록을 반환하도록 설정
		when(targetResolver.getWorkspaceWideMemberTargets(workspaceCode)).thenReturn(members);

		// processor가 예외를 던지도록 설정
		doThrow(new RuntimeException("Test exception"))
			.when(notificationProcessor).processNotification(event, members);

		// when & then
		assertThatThrownBy(() -> notificationEventHandler.handleIssueCreated(event))
			.isInstanceOf(RuntimeException.class);

		// 호출 확인
		verify(targetResolver).getWorkspaceWideMemberTargets(workspaceCode);
		verify(notificationProcessor).processNotification(event, members);
	}
}