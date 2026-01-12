package com.tissue.issue.application.service.event;

import com.tissue.common.dto.FieldChange;
import com.tissue.common.util.NullSafe;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueRelation;
import com.tissue.issue.domain.enums.ReviewStatus;
import com.tissue.issue.domain.event.IssueAssignedEvent;
import com.tissue.issue.domain.event.IssueCreatedEvent;
import com.tissue.issue.domain.event.IssueDeletedEvent;
import com.tissue.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.issue.domain.event.IssueParentChangedEvent;
import com.tissue.issue.domain.event.IssueRelationAddedEvent;
import com.tissue.issue.domain.event.IssueRelationRemovedEvent;
import com.tissue.issue.domain.event.IssueReporterChangedEvent;
import com.tissue.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.issue.domain.event.IssueReviewerRemovedEvent;
import com.tissue.issue.domain.event.IssueStoryPointChangedEvent;
import com.tissue.issue.domain.event.IssueTransitionedEvent;
import com.tissue.issue.domain.event.IssueUnassignedEvent;
import com.tissue.project.domain.ProjectMember;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishIssueCreated(Issue issue, ProjectMember actor) {
        eventPublisher.publishEvent(IssueCreatedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                issue.getParentKey(),
                issue.getParentId(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishIssueFieldsUpdated(Issue issue, Map<String, FieldChange> changes, ProjectMember actor) {
        eventPublisher.publishEvent(IssueFieldsUpdatedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                changes,
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishStoryPointChanged(Issue issue, @Nullable Integer oldStoryPoint, ProjectMember actor) {
        eventPublisher.publishEvent(IssueStoryPointChangedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                issue.getParentKey(),
                issue.getParentId(),
                oldStoryPoint,
                issue.getStoryPoint(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishParentChanged(
            Issue issue, @Nullable Issue oldParent, @Nullable Issue newParent, ProjectMember actor) {
        eventPublisher.publishEvent(IssueParentChangedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                NullSafe.get(oldParent, Issue::getKey),
                NullSafe.get(oldParent, Issue::getId),
                NullSafe.get(newParent, Issue::getKey),
                NullSafe.get(newParent, Issue::getId),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishIssueDeleted(Issue issue, ProjectMember actor) {
        eventPublisher.publishEvent(IssueDeletedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                issue.getParentKey(),
                issue.getParentId(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishReporterChanged(
            Issue issue, ProjectMember oldReporter, ProjectMember newReporter, ProjectMember actor) {
        eventPublisher.publishEvent(IssueReporterChangedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                oldReporter.getMemberId(),
                oldReporter.getDisplayName(),
                newReporter.getMemberId(),
                newReporter.getDisplayName(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishAssigned(Issue issue, ProjectMember assignee, ProjectMember actor) {
        eventPublisher.publishEvent(IssueAssignedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                assignee.getMemberId(),
                assignee.getDisplayName(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishUnassigned(Issue issue, ProjectMember removedAssignee, ProjectMember actor) {
        eventPublisher.publishEvent(IssueUnassignedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                removedAssignee.getMemberId(),
                removedAssignee.getDisplayName(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishReviewerAdded(Issue issue, ProjectMember reviewer, ProjectMember actor) {
        eventPublisher.publishEvent(IssueReviewerAddedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                reviewer.getMemberId(),
                reviewer.getDisplayName(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishReviewerRemoved(Issue issue, ProjectMember reviewer, ProjectMember actor) {
        eventPublisher.publishEvent(IssueReviewerRemovedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                reviewer.getMemberId(),
                reviewer.getDisplayName(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishRelationAdded(Issue source, Issue target, IssueRelation relation, ProjectMember actor) {
        eventPublisher.publishEvent(IssueRelationAddedEvent.create(
                source.getWorkspaceKey(),
                source.getProjectKey(),
                source.getKey(),
                source.getId(),
                target.getProjectKey(),
                target.getKey(),
                target.getId(),
                relation.getId(),
                relation.getRelationType(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishRelationRemoved(Issue source, Issue target, IssueRelation relation, ProjectMember actor) {
        eventPublisher.publishEvent(IssueRelationRemovedEvent.create(
                source.getWorkspaceKey(),
                source.getProjectKey(),
                source.getKey(),
                source.getId(),
                target.getProjectKey(),
                target.getKey(),
                target.getId(),
                relation.getId(),
                relation.getRelationType(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishReviewSubmitted(Issue issue, ReviewStatus status, ProjectMember actor) {
        eventPublisher.publishEvent(IssueReviewSubmittedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                status,
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishTransitioned(
            Issue issue, WorkflowTransition transition, WorkflowState oldState, ProjectMember actor) {
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
                actor.getMemberId(),
                actor.getDisplayName()));
    }
}
