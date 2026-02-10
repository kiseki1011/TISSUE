package com.tissue.comment;

import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ACTOR_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.CONTENT;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ISSUE_KEY;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.WORKSPACE_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import com.tissue.feature.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.feature.member.application.port.out.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.notification.application.port.out.NotificationRepository;
import com.tissue.feature.notification.application.service.NotificationTargetService;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.project.application.port.out.ProjectCommandRepository;
import com.tissue.feature.project.application.port.out.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.out.WorkspaceMemberContactInfo;
import com.tissue.feature.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.application.port.out.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.enums.SupportedLanguage;
import com.tissue.support.IntegrationTestSupport;
import com.tissue.support.email.EmailClient;
import java.time.Duration;
import java.util.ArrayList;
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
    WorkspaceRepository workspaceRepository;

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
        workspace = workspaceRepository.save(workspace);

        actor = saveWorkspaceMember(actorMember, WorkspaceRole.OWNER);
        saveWorkspaceMember(mentionedMember, WorkspaceRole.MEMBER);
        saveWorkspaceMember(participantMember, WorkspaceRole.MEMBER);

        project = Project.create(workspace, "TEST", "Test Project", "Test Description");
        project = projectCommandRepository.save(project);
        saveProjectMember(actorMember);
        saveProjectMember(mentionedMember);
        saveProjectMember(participantMember);
    }

    private WorkspaceMember saveWorkspaceMember(Member member, WorkspaceRole role) {
        return workspaceMemberCommandRepository.save(WorkspaceMember.create(member, workspace, role));
    }

    private void saveProjectMember(Member member) {
        WorkspaceMember wm = workspaceMemberQueryRepository
                .findByWorkspaceKeyAndMember_Id(workspace.getKey(), member.getId())
                .orElseThrow();

        projectMemberCommandRepository.save(ProjectMember.create(project, wm));
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
        WorkspaceMemberContactInfo participantInfo = new TestContactInfo(
                participantMember.getId(), participantMember.getEmail(), participantMember.getLanguage());

        WorkspaceMemberContactInfo mentionedInfo =
                new TestContactInfo(mentionedMember.getId(), mentionedMember.getEmail(), mentionedMember.getLanguage());

        doReturn(new HashSet<>(Set.of(participantInfo, mentionedInfo)))
                .when(targetService)
                .getIssueParticipantsAndReviewers(anyString(), anyString());

        doReturn(new ArrayList<>(List.of(mentionedInfo)))
                .when(targetService)
                .getMembersByUsernames(anyString(), anySet());

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

    record TestContactInfo(Long memberId, String email, SupportedLanguage language)
            implements WorkspaceMemberContactInfo {
        @Override
        public Long getMemberId() {
            return memberId;
        }

        @Override
        public String getEmail() {
            return email;
        }

        @Override
        public SupportedLanguage getLanguage() {
            return language;
        }
    }
}
