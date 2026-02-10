package com.tissue.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tissue.global.email.port.out.EmailClient;
import com.tissue.issue.domain.event.IssueAssignedEvent;
import com.tissue.issue.domain.event.IssueCreatedEvent;
import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.domain.Member;
import com.tissue.notification.application.port.out.NotificationRepository;
import com.tissue.notification.application.service.NotificationTargetService;
import com.tissue.notification.domain.Notification;
import com.tissue.project.application.port.out.ProjectCommandRepository;
import com.tissue.project.application.port.out.ProjectMemberCommandRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.support.IntegrationTestSupport;
import com.tissue.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberContactInfo;
import com.tissue.workspace.application.port.out.WorkspaceRepository;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

class NotificationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    NotificationRepository notificationRepository;

    @SpyBean
    NotificationTargetService targetService;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    MemberCommandRepository memberCommandRepository;

    @Autowired
    WorkspaceRepository workspaceRepository;

    @Autowired
    WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    @Autowired
    ProjectCommandRepository projectCommandRepository;

    @Autowired
    ProjectMemberCommandRepository projectMemberCommandRepository;

    @MockBean
    EmailClient emailClient;

    private Member actor;
    private Member targetMember;
    private Workspace workspace;
    private Project project;

    @BeforeEach
    void setupData() {
        // create Members
        actor = Member.create("actor@test.com", "actor", "Actor");
        actor = memberCommandRepository.save(actor);

        targetMember = Member.create("target@test.com", "target", "Target");
        targetMember = memberCommandRepository.save(targetMember);

        // create Workspace
        workspace = Workspace.create("TEST-WS", "Test Workspace", "Test Description");
        workspace = workspaceRepository.save(workspace);

        // add Members to Workspace
        WorkspaceMember actorWsMember = WorkspaceMember.create(actor, workspace, WorkspaceRole.OWNER);
        actorWsMember = workspaceMemberCommandRepository.save(actorWsMember);

        WorkspaceMember targetWsMember = WorkspaceMember.create(targetMember, workspace, WorkspaceRole.MEMBER);
        targetWsMember = workspaceMemberCommandRepository.save(targetWsMember);

        // create Project
        project = Project.create(workspace, "TEST", "Test Project", "Test Description");
        project = projectCommandRepository.save(project);

        // add Members to Project
        ProjectMember actorProjectMember = ProjectMember.create(project, actorWsMember);
        projectMemberCommandRepository.save(actorProjectMember);

        ProjectMember targetProjectMember = ProjectMember.create(project, targetWsMember);
        projectMemberCommandRepository.save(targetProjectMember);
    }

    @Test
    @DisplayName("Notification is sent to project members when IssueCreatedEvent occurs")
    void handleIssueCreated() {
        String issueKey = "TEST-1";
        IssueCreatedEvent event = IssueCreatedEvent.create(
                workspace.getKey(), project.getKey(), issueKey, null, actor.getId(), actor.getUsername());

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
    @DisplayName("Notification is sent to the assignee when IssueAssignedEvent occurs")
    void handleIssueAssigned() {
        WorkspaceMemberContactInfo contactInfo = mock(WorkspaceMemberContactInfo.class);
        when(contactInfo.getMemberId()).thenReturn(targetMember.getId());
        when(contactInfo.getEmail()).thenReturn(targetMember.getEmail());
        when(contactInfo.getLanguage()).thenReturn(targetMember.getLanguage());

        doReturn(new HashSet<>(Set.of(contactInfo))).when(targetService).getIssueAssignee(anyString(), anyString());

        String issueKey = "TEST-1";
        IssueAssignedEvent event = IssueAssignedEvent.create(
                workspace.getKey(),
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
}
