package com.tissue.feature.notification.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.tissue.feature.notification.application.dto.request.UpdateNotificationPreferenceCommand;
import com.tissue.feature.notification.application.port.repository.NotificationPreferenceRepository;
import com.tissue.feature.notification.domain.NotificationPreference;
import com.tissue.feature.notification.domain.enums.NotificationChannel;
import com.tissue.feature.notification.domain.enums.NotificationType;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    NotificationPreferenceRepository repository;

    @InjectMocks
    NotificationPreferenceService sut;

    @Nested
    @DisplayName("update preference")
    class UpdatePreference {

        @Test
        @DisplayName("success: creates new preference if not exists")
        void success_UpdatePreference() {
            // given
            Long memberId = 1L;
            UpdateNotificationPreferenceCommand command = new UpdateNotificationPreferenceCommand(
                    NotificationType.ISSUE_CREATED, NotificationChannel.EMAIL, false);

            given(repository.findByReceiverMemberId(memberId)).willReturn(Optional.empty());

            // when
            sut.updatePreference(command, memberId);

            // then
            then(repository).should().save(any(NotificationPreference.class));
        }
    }
}
