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
import com.tissue.api.issue.service.command.IssueReader;
import com.tissue.api.notification.service.command.NotificationCommandService;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

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

	@InjectMocks
	private NotificationEventHandler notificationEventHandler;

	@Test
	@DisplayName("이슈 생성 이벤트 발생 시 모든 워크스페이스 멤버에게 알림이 성공적으로 생성되어야 함")
	void handleIssueCreated_WhenAllNotificationsCreated_ShouldLogSuccess() {
		// given
		Long issueId = 1L;
		String issueKey = "ISSUE-1";
		String workspaceCode = "TESTCODE";
		Long actorId = 123L;

		// 이벤트 생성
		IssueCreatedEvent event = new IssueCreatedEvent(
			issueId,
			issueKey,
			workspaceCode,
			actorId
		);

		// Issue 모의 설정
		Issue issue = mock(Issue.class);
		when(issue.getId()).thenReturn(issueId);
		when(issue.getWorkspaceCode()).thenReturn(workspaceCode);
		when(issue.getTitle()).thenReturn("test issue");
		when(issue.getIssueKey()).thenReturn(issueKey);

		// IssueReader 모의 설정 - issueKey와 workspaceCode로 Issue 조회
		when(issueReader.findIssue(issueKey, workspaceCode)).thenReturn(issue);

		// Actor 모의 설정
		WorkspaceMember actor = mock(WorkspaceMember.class);
		when(actor.getNickname()).thenReturn("testuser");

		// 워크스페이스 멤버 모의 설정
		List<WorkspaceMember> members = Arrays.asList(
			mock(WorkspaceMember.class),
			mock(WorkspaceMember.class)
		);

		when(workspaceMemberReader.findWorkspaceMember(actorId)).thenReturn(actor);
		when(workspaceMemberRepository.findAllByWorkspaceCode(workspaceCode)).thenReturn(members);

		// when
		notificationEventHandler.handleIssueCreated(event);

		// then
		verify(notificationService, times(2)).createNotification(
			any(), any(), eq(workspaceCode), any(), any(), eq(issueId), any(), any(), any()
		);
	}

	@Test
	@DisplayName("일부 알림 생성이 실패해도 다른 멤버에 대한 알림 처리는 계속 진행되어야 한다")
	void handleIssueCreated_WhenNotificationFails_ShouldLogAndContinue() {
		// given
		Long issueId = 1L;
		String issueKey = "ISSUE-1";
		String workspaceCode = "TESTCODE";
		Long actorId = 123L;

		// 이벤트 생성
		IssueCreatedEvent event = new IssueCreatedEvent(
			issueId,
			issueKey,
			workspaceCode,
			actorId
		);

		// Issue 모의 설정
		Issue issue = mock(Issue.class);
		when(issue.getId()).thenReturn(issueId);
		when(issue.getWorkspaceCode()).thenReturn(workspaceCode);
		when(issue.getTitle()).thenReturn("test issue");
		when(issue.getIssueKey()).thenReturn(issueKey);

		// IssueReader 모의 설정
		when(issueReader.findIssue(issueKey, workspaceCode)).thenReturn(issue);

		// Actor 모의 설정
		WorkspaceMember actor = mock(WorkspaceMember.class);
		when(actor.getNickname()).thenReturn("testuser");

		// 워크스페이스 멤버 모의 설정
		List<WorkspaceMember> members = Arrays.asList(
			mock(WorkspaceMember.class),
			mock(WorkspaceMember.class)
		);

		when(workspaceMemberReader.findWorkspaceMember(actorId)).thenReturn(actor);
		when(workspaceMemberRepository.findAllByWorkspaceCode(workspaceCode)).thenReturn(members);

		// 첫 번째 알림 생성은 예외 발생, 두 번째는 성공하도록 설정
		doThrow(new RuntimeException("notification for first member failed"))
			.doNothing()
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
		Long issueId = 1L;
		String issueKey = "ISSUE-1";
		String workspaceCode = "TESTCODE";
		Long actorId = 123L;

		// 이벤트 생성
		IssueCreatedEvent event = new IssueCreatedEvent(
			issueId,
			issueKey,
			workspaceCode,
			actorId
		);

		// Issue 모의 설정
		Issue issue = mock(Issue.class);
		when(issue.getId()).thenReturn(issueId);
		when(issue.getWorkspaceCode()).thenReturn(workspaceCode);
		when(issue.getTitle()).thenReturn("test issue");
		when(issue.getIssueKey()).thenReturn(issueKey);

		// IssueReader 모의 설정
		when(issueReader.findIssue(issueKey, workspaceCode)).thenReturn(issue);

		// Actor 모의 설정
		WorkspaceMember actor = mock(WorkspaceMember.class);
		when(actor.getNickname()).thenReturn("testuser");

		// 워크스페이스 멤버 모의 설정
		List<WorkspaceMember> members = Arrays.asList(
			mock(WorkspaceMember.class),
			mock(WorkspaceMember.class)
		);

		when(workspaceMemberReader.findWorkspaceMember(actorId)).thenReturn(actor);
		when(workspaceMemberRepository.findAllByWorkspaceCode(workspaceCode)).thenReturn(members);

		// 모든 알림 생성 시 데이터 무결성 위반 예외 발생
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