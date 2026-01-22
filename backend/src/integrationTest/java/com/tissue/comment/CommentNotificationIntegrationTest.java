package com.tissue.comment;

import static com.tissue.notification.domain.constant.NotificationDataKeys.ACTOR_NAME;
import static com.tissue.notification.domain.constant.NotificationDataKeys.CONTENT;
import static com.tissue.notification.domain.constant.NotificationDataKeys.ISSUE_KEY;
import static com.tissue.notification.domain.constant.NotificationDataKeys.WORKSPACE_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import com.tissue.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.email.domain.EmailClient;
import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.domain.Member;
import com.tissue.notification.application.port.out.NotificationRepository;
import com.tissue.notification.application.service.NotificationTargetService;
import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.project.application.port.out.ProjectCommandRepository;
import com.tissue.project.application.port.out.ProjectMemberCommandRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.support.IntegrationTestSupport;
import com.tissue.workspace.application.port.out.WorkspaceCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberContact;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
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

class CommentNotificationIntegrationTest extends IntegrationTestSupport {

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
    WorkspaceCommandRepository workspaceCommandRepository;

    @Autowired
    WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    @Autowired
    ProjectMemberCommandRepository projectMemberCommandRepository;

    @Autowired
    ProjectCommandRepository projectCommandRepository;

    @Autowired
    WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    @MockBean
    EmailClient emailClient;

    private Member actorMember;
    private Member mentionedMember;
    private Member participantMember;
    private Workspace workspace;
    private Project project;

    private WorkspaceMember actor;

    @BeforeEach
    void setupData() {
        actorMember = Member.create("actor@test.com", "actor", "Actor");
        actorMember = memberCommandRepository.save(actorMember);

        mentionedMember = Member.create("mentioned@test.com", "mentionedUser", "Mentioned User");
        mentionedMember = memberCommandRepository.save(mentionedMember);

        participantMember = Member.create("participant@test.com", "participantUser", "Participant User");
        participantMember = memberCommandRepository.save(participantMember);

        workspace = Workspace.create("TEST-WS", "Test Workspace", "Test Description");
        workspace = workspaceCommandRepository.save(workspace);

        actor = saveWorkspaceMember(actorMember, WorkspaceRole.OWNER);
        saveWorkspaceMember(mentionedMember, WorkspaceRole.MEMBER);
        saveWorkspaceMember(participantMember, WorkspaceRole.MEMBER);

        project = Project.create(workspace, "TEST", "Test Project", "Test Description");
        project = projectCommandRepository.save(project);

        saveProjectMember(actorMember, ProjectRole.ADMIN);
        saveProjectMember(mentionedMember, ProjectRole.MEMBER);
        saveProjectMember(participantMember, ProjectRole.MEMBER);
    }

    private WorkspaceMember saveWorkspaceMember(Member member, WorkspaceRole role) {
        return workspaceMemberCommandRepository.save(WorkspaceMember.create(member, workspace, role));
    }

    private void saveProjectMember(Member member, ProjectRole role) {
        WorkspaceMember wm = workspaceMemberQueryRepository
                .findByMember_IdAndWorkspaceKey(member.getId(), workspace.getKey())
                .orElseThrow();
        projectMemberCommandRepository.save(ProjectMember.create(project, wm, role));
    }

    @Test
    @DisplayName("When a comment is added with mentions, mentioned users get ISSUE_MENTIONED "
            + "and others get ISSUE_COMMENT_ADDED notification")
    void handleIssueCommentAdded() {
        String issueKey = "TEST-1";
        Long commentId = 500L;
        String content = "Hello @mentionedUser, this is a test comment.";
        List<String> mentionedUsernames = List.of(mentionedMember.getUsername());

        // mock participants - assume mentioned user is also a participant
        doReturn(new HashSet<>(Set.of(
                        new WorkspaceMemberContact(
                                participantMember.getId(),
                                participantMember.getEmail(),
                                participantMember.getLanguage()),
                        new WorkspaceMemberContact(
                                mentionedMember.getId(), mentionedMember.getEmail(), mentionedMember.getLanguage()))))
                .when(targetService)
                .getIssueParticipantsAndReviewers(anyString(), anyString());

        IssueCommentAddedEvent event = IssueCommentAddedEvent.create(
                workspace.getKey(),
                project.getKey(),
                issueKey,
                commentId,
                content,
                mentionedUsernames,
                actorMember.getId(),
                actor.getDisplayName());

        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(event);
        });

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            // total notifications must be 2
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(2);

            Notification mentionNotification = notifications.stream()
                    .filter(n -> n.getType() == NotificationType.ISSUE_MENTIONED)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Mention notification not found"));

            assertThat(mentionNotification.getReceiverMemberId()).isEqualTo(mentionedMember.getId());
            assertThat(mentionNotification.getMessage().data())
                    .containsEntry(WORKSPACE_KEY, workspace.getKey())
                    .containsEntry(ISSUE_KEY, issueKey)
                    .containsEntry(ACTOR_NAME, actor.getDisplayName())
                    .containsEntry(CONTENT, content);

            Notification commentNotification = notifications.stream()
                    .filter(n -> n.getType() == NotificationType.ISSUE_COMMENT_ADDED)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Comment notification not found"));

            assertThat(commentNotification.getReceiverMemberId()).isEqualTo(participantMember.getId());
            assertThat(commentNotification.getMessage().data())
                    .containsEntry(WORKSPACE_KEY, workspace.getKey())
                    .containsEntry(ISSUE_KEY, issueKey)
                    .containsEntry(ACTOR_NAME, actor.getDisplayName())
                    .containsEntry(CONTENT, content);

            // verify that the mentioned user did not receive ISSUE_COMMENT_ADDED
            boolean mentionedUserReceivedIssueCommentAdded = notifications.stream()
                    .anyMatch(n -> n.getType() == NotificationType.ISSUE_COMMENT_ADDED
                            && n.getReceiverMemberId().equals(mentionedMember.getId()));

            assertThat(mentionedUserReceivedIssueCommentAdded).isFalse();
        });
    }
}
