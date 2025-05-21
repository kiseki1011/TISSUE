package com.tissue.unit.service.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.notification.application.service.command.NotificationCommandService;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.notification.domain.model.Notification;
import com.tissue.api.notification.domain.model.vo.NotificationMessage;
import com.tissue.api.notification.infrastructure.repository.NotificationRepository;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private WorkspaceMemberReader workspaceMemberReader;

	@InjectMocks
	private NotificationCommandService notificationCommandService;

	@Test
	@DisplayName("알림 생성 시 액터 정보를 조회하고 알림을 저장해야 함")
	void createNotification_ShouldSaveNotificationWithActorInfo() {
		// given
		UUID eventId = UUID.randomUUID();
		Long receiverId = 100L;
		String workspaceCode = "WORKSPACE1";
		NotificationType notificationType = NotificationType.ISSUE_CREATED;
		ResourceType resourceType = ResourceType.ISSUE;
		String title = "Test Title";
		String content = "Test Message";
		NotificationMessage message = new NotificationMessage(title, content);
		Long actorId = 300L;

		DomainEvent event = mock(DomainEvent.class);
		when(event.getEventId()).thenReturn(eventId);
		when(event.getActorMemberId()).thenReturn(actorId);
		when(event.getNotificationType()).thenReturn(notificationType);
		when(event.getWorkspaceCode()).thenReturn(workspaceCode);

		// 액터 모의 설정
		WorkspaceMember actorMember = mock(WorkspaceMember.class);
		when(actorMember.getDisplayName()).thenReturn("TestUser");
		when(workspaceMemberReader.findWorkspaceMember(actorId, workspaceCode)).thenReturn(actorMember);

		// when
		notificationCommandService.createNotification(
			event,
			receiverId,
			message
		);

		// then
		// 저장소에 Notification이 저장되었는지 검증
		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(notificationCaptor.capture());

		// 저장된 Notification 객체의 속성들이 올바르게 설정되었는지 검증
		Notification savedNotification = notificationCaptor.getValue();
		assertThat(savedNotification.getEventId()).isEqualTo(eventId);
		assertThat(savedNotification.getReceiverMemberId()).isEqualTo(receiverId);
		assertThat(savedNotification.getType()).isEqualTo(notificationType);
		assertThat(savedNotification.getTitle()).isEqualTo(message.title());
		assertThat(savedNotification.getContent()).isEqualTo(message.content());
		assertThat(savedNotification.getActorMemberId()).isEqualTo(actorId);
		assertThat(savedNotification.getActorDisplayName()).isEqualTo("TestUser");
		assertThat(savedNotification.isRead()).isFalse();
	}

	@Test
	@DisplayName("존재하는 알림을 읽음 표시할 수 있어야 함")
	void markAsRead_WithExistingNotification_ShouldMarkAsRead() {
		// given
		Long notificationId = 100L;
		Long workspaceMemberId = 200L;

		// 기존 알림 모의 설정
		Notification notification = mock(Notification.class);
		when(notificationRepository.findByIdAndReceiverMemberId(notificationId, workspaceMemberId))
			.thenReturn(Optional.of(notification));

		// when
		notificationCommandService.markAsRead(notificationId, workspaceMemberId);

		// then
		// markAsRead 메서드가 호출되었는지 검증
		verify(notification).markAsRead();
	}

	@Test
	@DisplayName("존재하지 않는 알림에 대해 읽음 표시 시 예외가 발생해야 함")
	void markAsRead_WithNonExistingNotification_ShouldThrowException() {
		// given
		Long notificationId = 100L;
		Long workspaceMemberId = 200L;

		// 존재하지 않는 알림 모의 설정
		when(notificationRepository.findByIdAndReceiverMemberId(notificationId, workspaceMemberId))
			.thenReturn(Optional.empty());

		// when & then
		// ResourceNotFoundException이 발생하는지 검증
		assertThatThrownBy(() -> notificationCommandService.markAsRead(notificationId, workspaceMemberId))
			.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("특정 워크스페이스의 모든 읽지 않은 알림을 읽음 표시할 수 있어야 함")
	void markAllAsRead_ShouldMarkAllUnreadNotifications() {
		// given
		Long workspaceMemberId = 100L;
		String workspaceCode = "WORKSPACE1";

		// 읽지 않은 알림 목록 모의 설정
		Notification notification1 = mock(Notification.class);
		Notification notification2 = mock(Notification.class);
		List<Notification> unreadNotifications = Arrays.asList(notification1, notification2);

		when(notificationRepository.findByReceiverMemberIdAndEntityReference_WorkspaceCodeAndIsReadFalse(
			workspaceMemberId, workspaceCode
		)).thenReturn(unreadNotifications);

		// when
		notificationCommandService.markAllAsRead(workspaceMemberId, workspaceCode);

		// then
		// 각 알림의 markAsRead 메서드가 호출되었는지 검증
		verify(notification1).markAsRead();
		verify(notification2).markAsRead();

		// saveAll 메서드가 호출되었는지 검증
		verify(notificationRepository).saveAll(unreadNotifications);
	}

	@Test
	@DisplayName("읽지 않은 알림이 없는 경우에도 오류 없이 처리되어야 함")
	void markAllAsRead_WithNoUnreadNotifications_ShouldHandleGracefully() {
		// given
		Long workspaceMemberId = 100L;
		String workspaceCode = "WORKSPACE1";

		// 빈 알림 목록 모의 설정
		List<Notification> emptyList = List.of();
		when(notificationRepository.findByReceiverMemberIdAndEntityReference_WorkspaceCodeAndIsReadFalse(
			workspaceMemberId, workspaceCode
		)).thenReturn(emptyList);

		// when
		notificationCommandService.markAllAsRead(workspaceMemberId, workspaceCode);

		// then
		// saveAll 메서드가 빈 리스트와 함께 호출되었는지 검증
		verify(notificationRepository).saveAll(emptyList);
	}
}