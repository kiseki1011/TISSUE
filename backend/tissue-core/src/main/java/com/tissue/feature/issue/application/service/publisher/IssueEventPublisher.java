package com.tissue.feature.issue.application.service.publisher;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueBranch;
import com.tissue.feature.issue.domain.IssueRelation;
import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.issue.domain.event.IssueAssignedEvent;
import com.tissue.feature.issue.domain.event.IssueBranchLinkedEvent;
import com.tissue.feature.issue.domain.event.IssueCreatedEvent;
import com.tissue.feature.issue.domain.event.IssueDeletedEvent;
import com.tissue.feature.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.feature.issue.domain.event.IssueParentChangedEvent;
import com.tissue.feature.issue.domain.event.IssueRelationAddedEvent;
import com.tissue.feature.issue.domain.event.IssueRelationRemovedEvent;
import com.tissue.feature.issue.domain.event.IssueRestoredEvent;
import com.tissue.feature.issue.domain.event.IssueReviewRequestedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewerRemovedEvent;
import com.tissue.feature.issue.domain.event.IssueStoryPointChangedEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedBySystemEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedEvent;
import com.tissue.feature.issue.domain.event.IssueUnassignedEvent;
import com.tissue.feature.issue.domain.event.IssueVcsConnectionEvent;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.shared.dto.FieldChange;
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

    public void publishVcsConnectionEvent(Issue issue, GitPrDto gitPr, @Nullable ProjectMember actor) {
        Long actorMemberId = (actor != null) ? actor.getMemberId() : null;
        String actorDisplayName = (actor != null) ? actor.getWorkspaceMember().getDisplayName() : null;

        eventPublisher.publishEvent(IssueVcsConnectionEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                gitPr.title(),
                gitPr.htmlUrl(),
                gitPr.action(),
                gitPr.authorEmail(),
                gitPr.authorUsername(),
                gitPr.occurredAt(),
                actorMemberId,
                actorDisplayName));
    }

    public void publishBranchLinked(Issue issue, IssueBranch branch, @Nullable ProjectMember actor) {
        Long actorMemberId = (actor != null) ? actor.getMemberId() : null;
        String actorDisplayName = (actor != null) ? actor.getWorkspaceMember().getDisplayName() : null;

        eventPublisher.publishEvent(IssueBranchLinkedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                branch.getBranchName(),
                branch.getRepoUrl(),
                branch.getPusherName(),
                actorMemberId,
                actorDisplayName));
    }

    public void publishIssueCreated(Issue issue, ProjectMember actor) {
        eventPublisher.publishEvent(IssueCreatedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getParentKey(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishIssueFieldsUpdated(Issue issue, Map<String, FieldChange> changes, ProjectMember actor) {
        eventPublisher.publishEvent(IssueFieldsUpdatedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                changes,
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishStoryPointChanged(Issue issue, @Nullable Integer oldStoryPoint, ProjectMember actor) {
        eventPublisher.publishEvent(IssueStoryPointChangedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getParentKey(),
                oldStoryPoint,
                issue.getStoryPoint(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishParentChanged(
            Issue issue, @Nullable Issue oldParent, @Nullable Issue newParent, ProjectMember actor) {
        eventPublisher.publishEvent(IssueParentChangedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                (oldParent != null) ? oldParent.getKey() : null,
                (newParent != null) ? newParent.getKey() : null,
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishIssueDeleted(Issue issue, ProjectMember actor) {
        eventPublisher.publishEvent(IssueDeletedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getParentKey(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishIssueRestored(Issue issue, ProjectMember actor) {
        eventPublisher.publishEvent(IssueRestoredEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getParentKey(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishAssigned(Issue issue, ProjectMember assignee, ProjectMember actor) {
        eventPublisher.publishEvent(IssueAssignedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                assignee.getMemberId(),
                assignee.getWorkspaceMember().getDisplayName(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishUnassigned(Issue issue, ProjectMember removedAssignee, ProjectMember actor) {
        eventPublisher.publishEvent(IssueUnassignedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                removedAssignee.getMemberId(),
                removedAssignee.getWorkspaceMember().getDisplayName(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishReviewerAdded(Issue issue, ProjectMember reviewer, ProjectMember actor) {
        eventPublisher.publishEvent(IssueReviewerAddedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                reviewer.getMemberId(),
                reviewer.getWorkspaceMember().getDisplayName(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishReviewerRemoved(Issue issue, ProjectMember reviewer, ProjectMember actor) {
        eventPublisher.publishEvent(IssueReviewerRemovedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                reviewer.getMemberId(),
                reviewer.getWorkspaceMember().getDisplayName(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishRelationAdded(
            Issue sourceIssue, Issue targetIssue, IssueRelation relation, ProjectMember actor) {
        eventPublisher.publishEvent(IssueRelationAddedEvent.create(
                sourceIssue.getWorkspaceKey(),
                sourceIssue.getProjectKey(),
                sourceIssue.getKey(),
                targetIssue.getProjectKey(),
                targetIssue.getKey(),
                relation.getId(),
                relation.getRelationType(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishRelationRemoved(
            Issue sourceIssue, Issue targetIssue, IssueRelation relation, ProjectMember actor) {
        eventPublisher.publishEvent(IssueRelationRemovedEvent.create(
                sourceIssue.getWorkspaceKey(),
                sourceIssue.getProjectKey(),
                sourceIssue.getKey(),
                targetIssue.getProjectKey(),
                targetIssue.getKey(),
                relation.getId(),
                relation.getRelationType(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishReviewSubmitted(Issue issue, ReviewStatus reviewStatus, ProjectMember actor) {
        eventPublisher.publishEvent(IssueReviewSubmittedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                reviewStatus,
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishReviewRequested(
            Issue issue, ProjectMember actor, @Nullable Set<Long> reviewerMemberIds, int reviewerCount) {
        eventPublisher.publishEvent(IssueReviewRequestedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName(),
                reviewerMemberIds,
                reviewerCount));
    }

    public void publishTransitioned(
            Issue issue, WorkflowTransition transition, WorkflowState oldState, ProjectMember actor) {
        eventPublisher.publishEvent(IssueTransitionedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getParentKey(),
                transition.getId(),
                transition.getDisplayName(),
                oldState.getId(),
                oldState.getDisplayName(),
                transition.getTargetState().getId(),
                transition.getTargetState().getDisplayName(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishTransitionedBySystem(
            Issue issue,
            WorkflowTransition transition,
            WorkflowState oldState,
            VcsProvider vcsProvider,
            @Nullable String vcsUserEmail,
            @Nullable String vcsUserName,
            String triggerReason) {

        eventPublisher.publishEvent(IssueTransitionedBySystemEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getParentKey(),
                transition.getId(),
                transition.getDisplayName(),
                oldState.getId(),
                oldState.getDisplayName(),
                transition.getTargetState().getId(),
                transition.getTargetState().getDisplayName(),
                vcsProvider,
                vcsUserEmail,
                vcsUserName,
                triggerReason));
    }
}
