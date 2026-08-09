package com.tissue.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tissue.feature.issue.domain.event.IssueAssignedEvent;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberContactInfo;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.notification.application.dto.response.NotificationResponse;
import com.tissue.feature.notification.application.port.email.EmailClient;
import com.tissue.feature.notification.application.port.repository.NotificationRepository;
import com.tissue.feature.notification.application.port.usecase.NotificationQueryUseCase;
import com.tissue.feature.notification.application.service.NotificationTargetService;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.notification.domain.vo.NotificationMessage;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.feature.project.domain.event.ProjectRoleChangedEvent;
import com.tissue.feature.realtime.application.RealtimeBroadcaster;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.vo.EntityReference;
import com.tissue.support.IntegrationTestSupport;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

class NotificationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    NotificationQueryUseCase notificationQueryUseCase;

    @MockitoSpyBean
    NotificationTargetService targetService;

    @MockitoSpyBean
    RealtimeBroadcaster broadcaster;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    MemberCommandRepository memberCommandRepository;

    @Autowired
    ProjectCommandRepository projectCommandRepository;

    @Autowired
    ProjectMemberCommandRepository projectMemberCommandRepository;

    @MockitoBean
    EmailClient emailClient;

    private Member actor;
    private Member targetMember;
    private Project project;

    @BeforeEach
    void setupData() {
        actor = memberCommandRepository.save(Member.create("actor@test.com", "actor", "Actor"));
        targetMember = memberCommandRepository.save(Member.create("target@test.com", "target", "Target"));

        project = projectCommandRepository.save(Project.create("TEST", "Test Project", "Test Description"));

        projectMemberCommandRepository.save(ProjectMember.createManager(project, actor));
        projectMemberCommandRepository.save(ProjectMember.create(project, targetMember));
    }

    @Test
    @DisplayName("Notification is sent to the assignee when IssueAssignedEvent occurs")
    void handleIssueAssigned() {
        MemberContactInfo contactInfo = mock(MemberContactInfo.class);
        when(contactInfo.getMemberId()).thenReturn(targetMember.getId());
        when(contactInfo.getEmail()).thenReturn(targetMember.getEmail());
        when(contactInfo.getLanguage()).thenReturn(targetMember.getLanguage());

        doReturn(new HashSet<>(Set.of(contactInfo))).when(targetService).getIssueAssignee(anyString());

        String issueKey = "TEST-1";
        IssueAssignedEvent event = IssueAssignedEvent.create(
                project.getKey(),
                issueKey,
                targetMember.getId(), // assignee
                targetMember.getUsername(),
                actor.getId(), // actor
                actor.getUsername());

        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(event);
        });

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(1);

            Notification notification = notifications.getFirst();
            assertThat(notification.getReceiverMemberId()).isEqualTo(targetMember.getId());
            assertThat(notification.getMessage().data().get("issueKey")).isEqualTo(issueKey);
        });
    }

    @Test
    @DisplayName("getNotifications filters by the given notification types, honouring unreadOnly")
    void getNotifications_filtersByTypes() {
        saveNotification(NotificationType.ISSUE_MENTIONED, "TEST-1");
        saveNotification(NotificationType.ISSUE_ASSIGNED, "TEST-2");

        CursorPage<NotificationResponse> mentionsOnly = notificationQueryUseCase.getNotifications(
                targetMember.getId(), false, List.of(NotificationType.ISSUE_MENTIONED), null, 20);
        assertThat(mentionsOnly.content()).hasSize(1);
        assertThat(mentionsOnly.content().getFirst().type()).isEqualTo(NotificationType.ISSUE_MENTIONED);

        CursorPage<NotificationResponse> unreadMentions = notificationQueryUseCase.getNotifications(
                targetMember.getId(), true, List.of(NotificationType.ISSUE_MENTIONED), null, 20);
        assertThat(unreadMentions.content()).hasSize(1);

        CursorPage<NotificationResponse> all =
                notificationQueryUseCase.getNotifications(targetMember.getId(), false, null, null, 20);
        assertThat(all.content()).hasSize(2);
    }

    @Test
    @DisplayName("A realtime 'notification' frame is pushed to the receiver when a notification is created")
    void realtimeBridgePushesToReceiver() {
        MemberContactInfo contactInfo = mock(MemberContactInfo.class);
        when(contactInfo.getMemberId()).thenReturn(targetMember.getId());
        when(contactInfo.getEmail()).thenReturn(targetMember.getEmail());
        when(contactInfo.getLanguage()).thenReturn(targetMember.getLanguage());
        doReturn(new HashSet<>(Set.of(contactInfo))).when(targetService).getIssueAssignee(anyString());

        IssueAssignedEvent event = IssueAssignedEvent.create(
                project.getKey(),
                "TEST-1",
                targetMember.getId(),
                targetMember.getUsername(),
                actor.getId(),
                actor.getUsername());

        transactionTemplate.executeWithoutResult(status -> publisher.publishEvent(event));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(broadcaster, atLeastOnce())
                .broadcastToMembers(
                        argThat((Set<Long> ids) -> ids.contains(targetMember.getId())),
                        eq("notification"),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq("NOTIFICATION_CREATED"),
                        any()));
    }

    private void saveNotification(NotificationType type, String issueKey) {
        notificationRepository.save(Notification.create(
                UUID.randomUUID(),
                type,
                EntityReference.forIssue(project.getKey(), issueKey),
                targetMember.getId(),
                targetMember.getEmail(),
                targetMember.getLanguage(),
                new NotificationMessage(Map.of("issueKey", issueKey)),
                actor.getId(),
                actor.getName()));
    }

    @Test
    @DisplayName("Notification is sent to the target member when ProjectRoleChangedEvent occurs")
    void handleProjectRoleChanged() {
        ProjectRoleChangedEvent event = ProjectRoleChangedEvent.create(
                project.getKey(),
                targetMember.getId(),
                targetMember.getName(),
                ProjectRole.MEMBER,
                ProjectRole.MANAGER,
                actor.getId(),
                actor.getName());

        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(event);
        });

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(1);

            Notification notification = notifications.getFirst();
            assertThat(notification.getReceiverMemberId()).isEqualTo(targetMember.getId());
            assertThat(notification.getNotificationType()).isEqualTo(NotificationType.PROJECT_ROLE_CHANGED);
            assertThat(notification.getMessage().data().get("newRole")).isEqualTo("MANAGER");
        });
    }
}
