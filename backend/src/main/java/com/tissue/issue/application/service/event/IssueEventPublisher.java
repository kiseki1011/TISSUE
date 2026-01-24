package com.tissue.issue.application.service.event;

import com.tissue.common.dto.FieldChange;
import com.tissue.common.util.NullSafe;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueBranch;
import com.tissue.issue.domain.IssueRelation;
import com.tissue.issue.domain.enums.ReviewStatus;
import com.tissue.issue.domain.event.IssueAssignedEvent;
import com.tissue.issue.domain.event.IssueBranchLinkedEvent;
import com.tissue.issue.domain.event.IssueCreatedEvent;
import com.tissue.issue.domain.event.IssueDeletedEvent;
import com.tissue.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.issue.domain.event.IssueParentChangedEvent;
import com.tissue.issue.domain.event.IssueRelationAddedEvent;
import com.tissue.issue.domain.event.IssueRelationRemovedEvent;
import com.tissue.issue.domain.event.IssueReporterChangedEvent;
import com.tissue.issue.domain.event.IssueReviewRequestedEvent;
import com.tissue.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.issue.domain.event.IssueReviewerRemovedEvent;
import com.tissue.issue.domain.event.IssueStoryPointChangedEvent;
import com.tissue.issue.domain.event.IssueTransitionedEvent;
import com.tissue.issue.domain.event.IssueUnassignedEvent;
import com.tissue.issue.domain.event.IssueVcsConnectionEvent;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.domain.ProjectMember;
import com.tissue.vcs.domain.GitPrDto;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishVcsConnectionEvent(Issue issue, GitPrDto gitPr) {
        eventPublisher.publishEvent(IssueVcsConnectionEvent.builder()
                .workspaceKey(issue.getWorkspaceKey())
                .projectKey(issue.getProjectKey())
                .issueKey(issue.getKey())
                .issueId(issue.getId())
                .prTitle(gitPr.title())
                .prUrl(gitPr.htmlUrl())
                .prAction(gitPr.action())
                .vcsUserEmail(gitPr.authorEmail())
                .vcsUserName(gitPr.authorUsername())
                .occurredAt(gitPr.occurredAt())
                .build());
    }

    public void publishBranchLinked(
            Issue issue, IssueBranch branch, @Nullable Long actorMemberId, @Nullable String actorDisplayName) {
        eventPublisher.publishEvent(IssueBranchLinkedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                branch.getBranchName(),
                branch.getRepoUrl(),
                branch.getPusherName(),
                actorMemberId,
                actorDisplayName));
    }

    public void publishIssueCreated(Issue issue, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueCreatedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getProject().getId(),
                issue.getKey(),
                issue.getId(),
                issue.getParentKey(),
                issue.getParentId(),
                actor.memberId(),
                actor.displayName()));
    }

    public void publishIssueFieldsUpdated(Issue issue, Map<String, FieldChange> changes, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueFieldsUpdatedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                changes,
                actor.memberId(),
                actor.displayName()));
    }

    public void publishStoryPointChanged(Issue issue, @Nullable Integer oldStoryPoint, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueStoryPointChangedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                issue.getParentKey(),
                issue.getParentId(),
                oldStoryPoint,
                issue.getStoryPoint(),
                actor.memberId(),
                actor.displayName()));
    }

    public void publishParentChanged(
            Issue issue, @Nullable Issue oldParent, @Nullable Issue newParent, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueParentChangedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                NullSafe.get(oldParent, Issue::getKey),
                NullSafe.get(oldParent, Issue::getId),
                NullSafe.get(newParent, Issue::getKey),
                NullSafe.get(newParent, Issue::getId),
                actor.memberId(),
                actor.displayName()));
    }

    public void publishIssueDeleted(Issue issue, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueDeletedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                issue.getParentKey(),
                issue.getParentId(),
                actor.memberId(),
                actor.displayName()));
    }

    public void publishReporterChanged(
            Issue issue, ProjectMember oldReporter, ProjectMember newReporter, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueReporterChangedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                oldReporter.getMemberId(),
                oldReporter.getWorkspaceMember().getDisplayName(),
                newReporter.getMemberId(),
                newReporter.getWorkspaceMember().getDisplayName(),
                actor.memberId(),
                actor.displayName()));
    }

    public void publishAssigned(Issue issue, ProjectMember assignee, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueAssignedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                assignee.getMemberId(),
                assignee.getWorkspaceMember().getDisplayName(),
                actor.memberId(),
                actor.displayName()));
    }

    public void publishUnassigned(Issue issue, ProjectMember removedAssignee, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueUnassignedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                removedAssignee.getMemberId(),
                removedAssignee.getWorkspaceMember().getDisplayName(),
                actor.memberId(),
                actor.displayName()));
    }

    public void publishReviewerAdded(Issue issue, ProjectMember reviewer, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueReviewerAddedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                reviewer.getMemberId(),
                reviewer.getWorkspaceMember().getDisplayName(),
                actor.memberId(),
                actor.displayName()));
    }

    public void publishReviewerRemoved(Issue issue, ProjectMember reviewer, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueReviewerRemovedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                reviewer.getMemberId(),
                reviewer.getWorkspaceMember().getDisplayName(),
                actor.memberId(),
                actor.displayName()));
    }

    public void publishRelationAdded(
            Issue sourceIssue, Issue targetIssue, IssueRelation relation, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueRelationAddedEvent.create(
                sourceIssue.getWorkspaceKey(),
                sourceIssue.getProjectKey(),
                sourceIssue.getKey(),
                sourceIssue.getId(),
                targetIssue.getProjectKey(),
                targetIssue.getKey(),
                targetIssue.getId(),
                relation.getId(),
                relation.getRelationType(),
                actor.memberId(),
                actor.displayName()));
    }

    public void publishRelationRemoved(
            Issue sourceIssue, Issue targetIssue, IssueRelation relation, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueRelationRemovedEvent.create(
                sourceIssue.getWorkspaceKey(),
                sourceIssue.getProjectKey(),
                sourceIssue.getKey(),
                sourceIssue.getId(),
                targetIssue.getProjectKey(),
                targetIssue.getKey(),
                targetIssue.getId(),
                relation.getId(),
                relation.getRelationType(),
                actor.memberId(),
                actor.displayName()));
    }

    public void publishReviewSubmitted(Issue issue, ReviewStatus reviewStatus, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueReviewSubmittedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                reviewStatus,
                actor.memberId(),
                actor.displayName()));
    }

    public void publishReviewRequested(
            Issue issue, ProjectMemberContext actor, @Nullable Set<Long> reviewerMemberIds, int reviewerCount) {
        eventPublisher.publishEvent(IssueReviewRequestedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                actor.memberId(),
                actor.displayName(),
                reviewerMemberIds,
                reviewerCount));
    }

    public void publishTransitioned(
            Issue issue, WorkflowTransition transition, WorkflowState oldState, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueTransitionedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                issue.getParentKey(),
                issue.getParentId(),
                transition.getId(),
                transition.getDisplayName(),
                oldState.getId(),
                oldState.getDisplayName(),
                transition.getTargetState().getId(),
                transition.getTargetState().getDisplayName(),
                actor.memberId(),
                actor.displayName()));
    }
}
