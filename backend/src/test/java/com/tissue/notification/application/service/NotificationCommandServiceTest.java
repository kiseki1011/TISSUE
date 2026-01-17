package com.tissue.notification.application.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.tissue.common.enums.SupportedLanguage;
import com.tissue.common.vo.EntityReference;
import com.tissue.notification.application.port.out.NotificationRepository;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.service.NotificationMessageFactory;
import com.tissue.notification.domain.vo.NotificationMessage;
import com.tissue.workspace.application.port.out.WorkspaceMemberContact;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @Mock
    NotificationRepository repository;

    @Mock
    NotificationMessageFactory messageFactory;

    @Mock
    NotificationProcessor processor;

    @InjectMocks
    NotificationCommandService sut;

    @Nested
    @DisplayName("create and send")
    class CreateAndSend {
        @Test
        @DisplayName("success: saves notifications and calls notification processor")
        void success_CreateAndSend() {
            UUID eventId = UUID.randomUUID();
            NotificationType type = NotificationType.ISSUE_CREATED;
            EntityReference ref = EntityReference.forIssue("TESTWS", "TESTPROJ", "TESTPROJ-1", 1L);
            WorkspaceMemberContact contact = new WorkspaceMemberContact(10L, "test@test.com", SupportedLanguage.EN);
            List<WorkspaceMemberContact> receivers = List.of(contact);
            Long actorId = 1L;
            String actorName = "Actor";
            Map<String, String> data = Map.of("key", "value");

            given(messageFactory.createMessage(type, data)).willReturn(new NotificationMessage(data));

            sut.createAndSend(eventId, type, ref, receivers, actorId, actorName, data);

            then(repository).should().saveAll(anyList());
            then(processor).should().process(anyList());
        }

        @Test
        @DisplayName("success: does nothing if receivers empty")
        void success_NoReceivers() {
            List<WorkspaceMemberContact> receivers = Collections.emptyList();

            sut.createAndSend(
                    UUID.randomUUID(), NotificationType.ISSUE_CREATED, null, receivers, 1L, "Actor", Map.of());

            then(repository).shouldHaveNoInteractions();
            then(processor).shouldHaveNoInteractions();
        }
    }
}
