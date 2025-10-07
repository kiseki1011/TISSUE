package deprecated.com.tissue.api.notification.application.service.command;

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tissue.api.notification.application.service.command.NotificationProcessor;
import com.tissue.api.notification.domain.enums.NotificationChannel;
import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.notification.domain.model.Notification;
import com.tissue.api.notification.domain.model.NotificationPreference;
import com.tissue.api.notification.domain.model.vo.EntityReference;
import com.tissue.api.notification.domain.service.sender.NotificationSender;
import com.tissue.api.notification.infrastructure.repository.NotificationPreferenceRepository;

@ExtendWith(MockitoExtension.class)
class NotificationProcessorTest {
	@Mock
	private NotificationPreferenceRepository preferenceRepository;

	@Mock
	private NotificationSender inAppSender;

	@Mock
	private NotificationSender emailSender;

	@InjectMocks
	private NotificationProcessor notificationProcessor;

	@BeforeEach
	void setup() {
		when(inAppSender.getChannel()).thenReturn(NotificationChannel.IN_APP);
		when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);

		// injectMocks 안에서 직접 sender list 주입이 안 되므로
		notificationProcessor = new NotificationProcessor(
			List.of(inAppSender, emailSender), preferenceRepository
		);
	}

	@Test
	@DisplayName("설정이 없으면 기본적으로 전송된다")
	void process_shouldSend_WhenPreferenceNotExists() {
		// given
		Notification notification = mock(Notification.class);
		when(notification.getReceiverMemberId()).thenReturn(1L);
		when(notification.getType()).thenReturn(NotificationType.ISSUE_CREATED);
		when(notification.getEntityReference()).thenReturn(
			EntityReference.builder()
				.workspaceCode("TESTCODE")
				.resourceType(ResourceType.ISSUE)
				.stringKey("ISSUE-1")
				.build());

		when(preferenceRepository.findByReceiver(any(), any(), any(), any()))
			.thenReturn(Optional.empty());

		// when
		notificationProcessor.process(notification);

		// then
		verify(inAppSender).send(notification);
		verify(emailSender).send(notification);
	}

	@Test
	@DisplayName("설정에서 이메일을 비허용하면 인앱 알림만 발송된다")
	void process_shouldOnlySendEmail_WhenPreferenceDisallowsInApp() {
		// given
		Notification notification = mock(Notification.class);
		when(notification.getReceiverMemberId()).thenReturn(1L);
		when(notification.getType()).thenReturn(NotificationType.ISSUE_CREATED);
		when(notification.getEntityReference()).thenReturn(
			EntityReference.builder()
				.workspaceCode("TESTCODE")
				.resourceType(ResourceType.ISSUE)
				.stringKey("ISSUE-1")
				.build());

		when(preferenceRepository.findByReceiver(any(), any(), any(), eq(NotificationChannel.IN_APP)))
			.thenReturn(Optional.of(
				new NotificationPreference(1L, "TEST", NotificationType.ISSUE_CREATED, NotificationChannel.IN_APP,
					true)));

		when(preferenceRepository.findByReceiver(any(), any(), any(), eq(NotificationChannel.EMAIL)))
			.thenReturn(Optional.of(
				new NotificationPreference(1L, "TEST", NotificationType.ISSUE_CREATED, NotificationChannel.EMAIL,
					false)));

		// when
		notificationProcessor.process(notification);

		// then
		verify(inAppSender).send(notification);
		verify(emailSender, never()).send(any());
	}

	@Test
	@DisplayName("설정에서 모두 차단된 경우 아무 채널도 전송되지 않는다")
	void process_shouldSendNothing_WhenAllChannelsDisabled() {
		// given
		Notification notification = mock(Notification.class);
		when(notification.getReceiverMemberId()).thenReturn(1L);
		when(notification.getType()).thenReturn(NotificationType.ISSUE_CREATED);
		when(notification.getEntityReference()).thenReturn(
			EntityReference.builder()
				.workspaceCode("TESTCODE")
				.resourceType(ResourceType.ISSUE)
				.stringKey("ISSUE-1")
				.build());

		when(preferenceRepository.findByReceiver(any(), any(), any(), eq(NotificationChannel.IN_APP)))
			.thenReturn(Optional.of(
				new NotificationPreference(1L, "TEST", NotificationType.ISSUE_CREATED, NotificationChannel.IN_APP,
					false)));

		when(preferenceRepository.findByReceiver(any(), any(), any(), eq(NotificationChannel.EMAIL)))
			.thenReturn(Optional.of(
				new NotificationPreference(1L, "TEST", NotificationType.ISSUE_CREATED, NotificationChannel.EMAIL,
					false)));

		// when
		notificationProcessor.process(notification);

		// then
		verify(inAppSender, never()).send(any());
		verify(emailSender, never()).send(any());
	}
}