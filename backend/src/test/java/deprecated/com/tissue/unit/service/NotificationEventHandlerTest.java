package deprecated.com.tissue.unit.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {

	// @Test
	// @DisplayName("이슈 생성 이벤트 발생 시 워크스페이스 멤버들에게 알림이 처리되어야 함")
	// void handleIssueCreated_ShouldProcessNotificationForAllWorkspaceMembers() {
	// 	// given
	// 	String workspaceKey = "TESTCODE";
	// 	Long actorId = 123L;
	//
	// 	Issue issue = mock(Issue.class);
	// 	when(issue.getIssueKey()).thenReturn("ISSUE-1");
	// 	when(issue.getWorkspaceCode()).thenReturn(workspaceKey);
	// 	when(issue.getType()).thenReturn(IssueType.STORY);
	//
	// 	IssueCreatedEvent event = IssueCreatedEvent.createEvent(issue, actorId);
	//
	// 	WorkspaceMember wm1 = mock(WorkspaceMember.class);
	// 	WorkspaceMember wm2 = mock(WorkspaceMember.class);
	// 	when(wm1.getMember()).thenReturn(mock(Member.class));
	// 	when(wm2.getMember()).thenReturn(mock(Member.class));
	// 	List<WorkspaceMember> members = List.of(wm1, wm2);
	//
	// 	when(targetService.getWorkspaceWideMemberTargets(workspaceKey)).thenReturn(members);
	//
	// 	NotificationMessage msg = new NotificationMessage("title", "body");
	// 	when(notificationMessageFactory.createMessage(event)).thenReturn(msg);
	//
	// 	Notification notification1 = mock(Notification.class);
	// 	Notification notification2 = mock(Notification.class);
	//
	// 	when(notificationService.createNotification(eq(event), anyLong(), eq(msg)))
	// 		.thenReturn(notification1, notification2);
	//
	// 	// when
	// 	notificationEventHandler.handleIssueCreated(event);
	//
	// 	// then
	// 	verify(notificationService, times(2)).createNotification(eq(event), anyLong(), eq(msg));
	// 	verify(notificationProcessor, times(2)).process(any(Notification.class));
	// }
}