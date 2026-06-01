package com.tissue.feature.comment;

import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ACTOR_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.CONTENT;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ISSUE_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import com.tissue.feature.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberContactInfo;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.notification.application.port.email.EmailClient;
import com.tissue.feature.notification.application.port.repository.NotificationRepository;
import com.tissue.feature.notification.application.service.NotificationTargetService;
import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.enums.SupportedLanguage;
import com.tissue.support.IntegrationTestSupport;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

class CommentNotificationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    NotificationRepository notificationRepository;

    @MockitoSpyBean
    NotificationTargetService targetService;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    MemberCommandRepository memberCommandRepository;

    @Autowired
    ProjectMemberCommandRepository projectMemberCommandRepository;

    @Autowired
    ProjectCommandRepository projectCommandRepository;

    @MockitoBean
    EmailClient emailClient;

    private Member actorMember;

    private Member mentionedMember;

    private Member participantMember;

    private Project project;

    @BeforeEach
    void setupData() {
        actorMember = memberCommandRepository.save(Member.create("actor@test.com", "actor", "ActorMember"));
        mentionedMember =
                memberCommandRepository.save(Member.create("mentioned@test.com", "mentioneduser", "MentionedUser"));
        participantMember = memberCommandRepository.save(
                Member.create("participant@test.com", "participantuser", "ParticipantUser"));

        project = projectCommandRepository.save(Project.create("TEST", "Test Project", "Test Description"));
        projectMemberCommandRepository.save(ProjectMember.createManager(project, actorMember));
        projectMemberCommandRepository.save(ProjectMember.create(project, mentionedMember));
        projectMemberCommandRepository.save(ProjectMember.create(project, participantMember));
    }

    @Test
    @DisplayName("when a comment is added with mentions, mentioned users get ISSUE_MENTIONED "
            + "and others get ISSUE_COMMENT_ADDED notification")
    void handleIssueCommentAdded() {
        String issueKey = "TEST-1";
        Long commentId = 500L;
        String content = "Hello @mentioneduser, this is a test comment.";

        List<String> mentionedUsernames = List.of(mentionedMember.getUsername());

        // mock participants - assume mentioned user is also a participant
        MemberContactInfo participantInfo = new TestContactInfo(
                participantMember.getId(), participantMember.getEmail(), participantMember.getLanguage());

        MemberContactInfo mentionedInfo =
                new TestContactInfo(mentionedMember.getId(), mentionedMember.getEmail(), mentionedMember.getLanguage());

        doReturn(new HashSet<>(Set.of(participantInfo, mentionedInfo)))
                .when(targetService)
                .getIssueParticipantsAndReviewers(anyString());

        doReturn(new ArrayList<>(List.of(mentionedInfo))).when(targetService).getMembersByUsernames(anySet());

        IssueCommentAddedEvent event = IssueCommentAddedEvent.create(
                project.getKey(),
                issueKey,
                commentId,
                content,
                mentionedUsernames,
                actorMember.getId(),
                actorMember.getName());

        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(event);
        });

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            // total notifications must be 2
            List<Notification> notifications = notificationRepository.findAll();

            assertThat(notifications).hasSize(2);

            Notification mentionNotification = notifications.stream()
                    .filter(n -> n.getNotificationType() == NotificationType.ISSUE_MENTIONED)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Mention notification not found"));

            assertThat(mentionNotification.getReceiverMemberId()).isEqualTo(mentionedMember.getId());

            assertThat(mentionNotification.getMessage().data())
                    .containsEntry(ISSUE_KEY, issueKey)
                    .containsEntry(ACTOR_NAME, actorMember.getName())
                    .containsEntry(CONTENT, content);

            Notification commentNotification = notifications.stream()
                    .filter(n -> n.getNotificationType() == NotificationType.ISSUE_COMMENT_ADDED)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Comment notification not found"));

            assertThat(commentNotification.getReceiverMemberId()).isEqualTo(participantMember.getId());

            assertThat(commentNotification.getMessage().data())
                    .containsEntry(ISSUE_KEY, issueKey)
                    .containsEntry(ACTOR_NAME, actorMember.getName())
                    .containsEntry(CONTENT, content);

            // verify that the mentioned user did not receive ISSUE_COMMENT_ADDED

            boolean mentionedUserReceivedIssueCommentAdded = notifications.stream()
                    .anyMatch(n -> n.getNotificationType() == NotificationType.ISSUE_COMMENT_ADDED
                            && n.getReceiverMemberId().equals(mentionedMember.getId()));

            assertThat(mentionedUserReceivedIssueCommentAdded).isFalse();
        });
    }

    record TestContactInfo(Long memberId, String email, SupportedLanguage language) implements MemberContactInfo {
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
