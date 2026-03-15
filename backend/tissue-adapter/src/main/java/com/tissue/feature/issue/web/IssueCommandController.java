package com.tissue.feature.issue.web;

import com.tissue.feature.issue.application.dto.response.IssueCreateResponse;
import com.tissue.feature.issue.application.port.usecase.IssueLifecycleUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueParticipantUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueRelationUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueReviewUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueTagUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueTransitionUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueUpdateUseCase;
import com.tissue.feature.issue.web.request.AddIssueRelationRequest;
import com.tissue.feature.issue.web.request.AssignParentIssueRequest;
import com.tissue.feature.issue.web.request.BatchChangeParentRequest;
import com.tissue.feature.issue.web.request.BatchRemoveParentRequest;
import com.tissue.feature.issue.web.request.BatchSoftDeleteRequest;
import com.tissue.feature.issue.web.request.CreateIssueRequest;
import com.tissue.feature.issue.web.request.PerformTransitionRequest;
import com.tissue.feature.issue.web.request.RemoveIssueRelationRequest;
import com.tissue.feature.issue.web.request.RequestReviewRequest;
import com.tissue.feature.issue.web.request.SubmitReviewRequest;
import com.tissue.feature.issue.web.request.UpdateCommonFieldsRequest;
import com.tissue.feature.issue.web.request.UpdateCustomFieldsRequest;
import com.tissue.feature.issue.web.request.UpdateStoryPointRequest;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import com.tissue.shared.dto.BatchOperationResponse;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}")
@RequiredArgsConstructor
public class IssueCommandController {

    private final IssueLifecycleUseCase lifecycleUseCase;
    private final IssueUpdateUseCase updateUseCase;
    private final IssueTransitionUseCase transitionUseCase;
    private final IssueParticipantUseCase participantUseCase;
    private final IssueRelationUseCase relationUseCase;
    private final IssueReviewUseCase reviewUseCase;
    private final IssueTagUseCase tagUseCase;

    @PostMapping("/issues")
    public ResponseEntity<IssueCreateResponse> create(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid CreateIssueRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        IssueCreateResponse response = lifecycleUseCase.create(
                ProjectIdentifier.of(workspaceKey, projectKey), command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/issues/{issueKey}")
    public ResponseEntity<Void> updateCommonFields(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCommonFieldsRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        updateUseCase.updateCommonFields(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/issues/{issueKey}/custom")
    public ResponseEntity<Void> updateCustomFields(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCustomFieldsRequest request,
            @CurrentMember MemberDetails memberDetails) {

        updateUseCase.updateCustomFields(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey),
                request.customFields(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/issues/{issueKey}/storypoint")
    public ResponseEntity<Void> updateStoryPoint(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateStoryPointRequest request,
            @CurrentMember MemberDetails memberDetails) {

        updateUseCase.updateStoryPoint(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey),
                request.storyPoint(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/issues/{issueKey}/parent")
    public ResponseEntity<Void> assignParent(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid AssignParentIssueRequest request,
            @CurrentMember MemberDetails memberDetails) {

        updateUseCase.assignParent(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey),
                request.parentIssueKey(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/{issueKey}/parent")
    public ResponseEntity<Void> removeParent(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        updateUseCase.removeParent(IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/issues/batch/parent")
    public ResponseEntity<BatchOperationResponse> batchChangeParent(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid BatchChangeParentRequest request,
            @CurrentMember MemberDetails memberDetails) {

        BatchOperationResponse response = updateUseCase.batchAssignParent(
                ProjectIdentifier.of(workspaceKey, projectKey), request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/issues/batch/parent")
    public ResponseEntity<BatchOperationResponse> batchRemoveParent(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid BatchRemoveParentRequest request,
            @CurrentMember MemberDetails memberDetails) {

        BatchOperationResponse response = updateUseCase.batchRemoveParent(
                ProjectIdentifier.of(workspaceKey, projectKey), request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/issues/{issueKey}/transitions/{transitionId}")
    public ResponseEntity<Void> performTransition(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid PerformTransitionRequest request,
            @CurrentMember MemberDetails memberDetails) {

        transitionUseCase.performTransition(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey),
                request.transitionId(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/{issueKey}")
    public ResponseEntity<Void> softDelete(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        lifecycleUseCase.delete(IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/restore")
    public ResponseEntity<Void> restore(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        lifecycleUseCase.restore(IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/batch")
    public ResponseEntity<BatchOperationResponse> batchSoftDelete(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid BatchSoftDeleteRequest request,
            @CurrentMember MemberDetails memberDetails) {

        BatchOperationResponse response = lifecycleUseCase.batchSoftDelete(
                ProjectIdentifier.of(workspaceKey, projectKey), request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/issues/{issueKey}/assignees/{memberId}")
    public ResponseEntity<Void> assign(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @PathVariable Long memberId,
            @CurrentMember MemberDetails memberDetails) {

        participantUseCase.assign(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/{issueKey}/assignees")
    public ResponseEntity<Void> unassign(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        participantUseCase.unassign(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/subscribers")
    public ResponseEntity<Void> subscribe(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        participantUseCase.subscribe(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/{issueKey}/subscribers")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        participantUseCase.unsubscribe(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/reviewers/{targetMemberId}")
    public ResponseEntity<Void> addReviewer(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {

        participantUseCase.addReviewer(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), targetMemberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/{issueKey}/reviewers/{targetMemberId}")
    public ResponseEntity<Void> removeReviewer(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {

        participantUseCase.removeReviewer(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), targetMemberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/relations")
    public ResponseEntity<Void> addRelation(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid AddIssueRelationRequest request,
            @CurrentMember MemberDetails memberDetails) {

        relationUseCase.add(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey),
                request.targetIssueKey(),
                request.relationType(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/{sourceIssueKey}/relations")
    public ResponseEntity<Void> removeRelation(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String sourceIssueKey,
            @RequestBody @Valid RemoveIssueRelationRequest request,
            @CurrentMember MemberDetails memberDetails) {

        relationUseCase.remove(
                IssueIdentifier.of(workspaceKey, projectKey, sourceIssueKey),
                request.targetIssueKey(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/review")
    public ResponseEntity<Void> requestReview(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid RequestReviewRequest request,
            @CurrentMember MemberDetails memberDetails) {

        reviewUseCase.requestReview(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey),
                request.reviewerMemberIds(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/reviews/submit")
    public ResponseEntity<Void> submitReview(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid SubmitReviewRequest request,
            @CurrentMember MemberDetails memberDetails) {

        reviewUseCase.submitReview(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey),
                request.approved(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/tags/{tagId}")
    public ResponseEntity<Void> addTag(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @PathVariable Long tagId,
            @CurrentMember MemberDetails memberDetails) {

        tagUseCase.addTag(IssueIdentifier.of(workspaceKey, projectKey, issueKey), tagId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/{issueKey}/tags/{tagId}")
    public ResponseEntity<Void> removeTag(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @PathVariable Long tagId,
            @CurrentMember MemberDetails memberDetails) {

        tagUseCase.removeTag(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), tagId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
