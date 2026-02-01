package com.tissue.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.tissue.notification.adapter.web.request.UpdateNotificationPreferenceRequest;
import com.tissue.notification.application.port.out.NotificationPreferenceRepository;
import com.tissue.notification.domain.NotificationPreference;
import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;
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
    @DisplayName("get preferences")
    class GetPreferences {

        @Test
        @DisplayName("success: returns all types except IN_APP")
        void success_GetPreferences() {
            // Given
            String workspaceKey = "TESTWS";
            Long memberId = 1L;
            given(repository.findByReceiverMemberIdAndWorkspaceKey(memberId, workspaceKey))
                .willReturn(Optional.empty());

            // When
            var result = sut.getPreferences(workspaceKey, memberId);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).noneMatch(r -> r.channel() == NotificationChannel.IN_APP);
        }
    }

    @Nested
    @DisplayName("update preference")
    class UpdatePreference {

        @Test
        @DisplayName("success: creates new preference if not exists")
        void success_UpdatePreference() {
            // Given
            String workspaceKey = "TESTWS";
            Long memberId = 1L;
            UpdateNotificationPreferenceRequest req = new UpdateNotificationPreferenceRequest(
                NotificationType.ISSUE_CREATED, NotificationChannel.EMAIL, false);

            given(repository.findByReceiverMemberIdAndWorkspaceKey(memberId, workspaceKey))
                .willReturn(Optional.empty());

            // When
            sut.updatePreference(workspaceKey, memberId, req);

            // Then
            then(repository).should().save(any(NotificationPreference.class));
        }
    }
}
