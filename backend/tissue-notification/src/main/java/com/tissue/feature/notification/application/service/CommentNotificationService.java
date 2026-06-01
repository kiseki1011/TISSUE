package com.tissue.feature.notification.application.service;

import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ACTOR_NAME;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.CONTENT;
import static com.tissue.feature.notification.domain.constant.NotificationDataKeys.ISSUE_KEY;

import com.tissue.feature.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.feature.comment.domain.event.IssueCommentUpdatedEvent;
import com.tissue.feature.member.application.port.repository.MemberContactInfo;
import com.tissue.feature.notification.application.port.usecase.CommentNotificationUseCase;
import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.shared.vo.EntityReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentNotificationService implements CommentNotificationUseCase {

    private final NotificationCommandService commandService;
    private final NotificationTargetService targetService;

    @Override
    public void handleIssueCommentAdded(IssueCommentAddedEvent event) {
        List<MemberContactInfo> mentionedMembers =
                targetService.getMembersByUsernames(new HashSet<>(event.mentionedUsernames()));

        removeActorFromTargets(mentionedMembers, event.actorMemberId());

        Set<MemberContactInfo> participants = targetService.getIssueParticipantsAndReviewers(event.issueKey());

        removeActorFromTargets(participants, event.actorMemberId());
        removeMentionedFromParticipants(mentionedMembers, participants);

        log.debug(
                "Handling IssueCommentAddedEvent: issue={}, mentioned={}, participants={}",
                event.issueKey(),
                mentionedMembers.size(),
                participants.size());

        EntityReference reference =
                EntityReference.forIssueComment(event.projectKey(), event.issueKey(), event.commentId());

        if (!mentionedMembers.isEmpty()) {
            commandService.createAndSend(
                    event.eventId(),
                    NotificationType.ISSUE_MENTIONED,
                    reference,
                    mentionedMembers,
                    event.actorMemberId(),
                    event.actorDisplayName(),
                    Map.of(
                            ISSUE_KEY,
                            event.issueKey(),
                            ACTOR_NAME,
                            event.actorDisplayName(),
                            CONTENT,
                            event.content()));
        }

        if (!participants.isEmpty()) {
            commandService.createAndSend(
                    event.eventId(),
                    NotificationType.ISSUE_COMMENT_ADDED,
                    reference,
                    participants,
                    event.actorMemberId(),
                    event.actorDisplayName(),
                    Map.of(
                            ISSUE_KEY,
                            event.issueKey(),
                            ACTOR_NAME,
                            event.actorDisplayName(),
                            CONTENT,
                            event.content()));
        }
    }

    @Override
    public void handleIssueCommentUpdated(IssueCommentUpdatedEvent event) {
        List<MemberContactInfo> mentionedMembers =
                targetService.getMembersByUsernames(new HashSet<>(event.mentionedUsernames()));

        removeActorFromTargets(mentionedMembers, event.actorMemberId());

        Set<MemberContactInfo> participants = targetService.getIssueParticipantsAndReviewers(event.issueKey());

        removeActorFromTargets(participants, event.actorMemberId());
        removeMentionedFromParticipants(mentionedMembers, participants);

        log.debug(
                "Handling IssueCommentUpdatedEvent: issue={}, mentioned={}, participants={}",
                event.issueKey(),
                mentionedMembers.size(),
                participants.size());

        EntityReference reference =
                EntityReference.forIssueComment(event.projectKey(), event.issueKey(), event.commentId());

        if (!mentionedMembers.isEmpty()) {
            commandService.createAndSend(
                    event.eventId(),
                    NotificationType.ISSUE_MENTIONED,
                    reference,
                    mentionedMembers,
                    event.actorMemberId(),
                    event.actorDisplayName(),
                    Map.of(
                            ISSUE_KEY,
                            event.issueKey(),
                            ACTOR_NAME,
                            event.actorDisplayName(),
                            CONTENT,
                            event.content()));
        }

        if (!participants.isEmpty()) {
            commandService.createAndSend(
                    event.eventId(),
                    NotificationType.ISSUE_COMMENT_UPDATED,
                    reference,
                    participants,
                    event.actorMemberId(),
                    event.actorDisplayName(),
                    Map.of(
                            ISSUE_KEY,
                            event.issueKey(),
                            ACTOR_NAME,
                            event.actorDisplayName(),
                            CONTENT,
                            event.content()));
        }
    }

    private void removeMentionedFromParticipants(
            List<MemberContactInfo> mentionedMembers, Set<MemberContactInfo> participants) {
        Set<Long> mentionedMemberIds =
                mentionedMembers.stream().map(MemberContactInfo::getMemberId).collect(Collectors.toSet());
        participants.removeIf(p -> mentionedMemberIds.contains(p.getMemberId()));
    }

    private void removeActorFromTargets(Collection<MemberContactInfo> targets, Long memberId) {
        if (targets == null || memberId == null) {
            return;
        }
        targets.removeIf(target -> target.getMemberId().equals(memberId));
    }
}
