package deprecated.com.tissue.api.notification.application.service.command;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

	// @Mock
	// private NotificationPreferenceRepository repository;
	//
	// @InjectMocks
	// private NotificationPreferenceService service;
	//
	// @Test
	// @DisplayName("기존 알림 설정이 존재하지 않으면, 새로 생성한다")
	// void updatePreference_shouldCreateNewPreferenceIfNotExists() {
	// 	// given
	// 	String workspaceCode = "WS123";
	// 	Long memberId = 1L;
	// 	NotificationType type = NotificationType.ISSUE_CREATED;
	//
	// 	UpdateNotificationPreferenceRequest request = new UpdateNotificationPreferenceRequest(type, true);
	//
	// 	when(repository.findByReceiver(memberId, workspaceCode, type, NotificationChannel.EMAIL))
	// 		.thenReturn(Optional.empty());
	//
	// 	// when
	// 	service.updatePreference(workspaceCode, memberId, request);
	//
	// 	// then
	// 	ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
	// 	verify(repository).save(captor.capture());
	//
	// 	NotificationPreference saved = captor.getValue();
	// 	assertThat(saved.getReceiverMemberId()).isEqualTo(memberId);
	// 	assertThat(saved.getWorkspaceKey()).isEqualTo(workspaceCode);
	// 	assertThat(saved.getType()).isEqualTo(type);
	// 	assertThat(saved.getChannel()).isEqualTo(NotificationChannel.EMAIL);
	// 	assertThat(saved.isEnabled()).isTrue();
	// }
	//
	// @Test
	// @DisplayName("알림 설정을 업데이트할 수 있다")
	// void updatePreference_shouldUpdateExistingPreference() {
	// 	// given
	// 	String workspaceCode = "WS123";
	// 	Long memberId = 1L;
	// 	NotificationType type = NotificationType.ISSUE_CREATED;
	//
	// 	NotificationPreference existing = NotificationPreference.builder()
	// 		.receiverMemberId(memberId)
	// 		.workspaceKey(workspaceCode)
	// 		.type(type)
	// 		.channel(NotificationChannel.EMAIL)
	// 		.enabled(false)
	// 		.build();
	//
	// 	UpdateNotificationPreferenceRequest request = new UpdateNotificationPreferenceRequest(type, true);
	//
	// 	when(repository.findByReceiver(memberId, workspaceCode, type, NotificationChannel.EMAIL))
	// 		.thenReturn(Optional.of(existing));
	//
	// 	// when
	// 	service.updatePreference(workspaceCode, memberId, request);
	//
	// 	// then
	// 	assertThat(existing.isEnabled()).isTrue();
	// 	verify(repository).save(existing);
	// }
}