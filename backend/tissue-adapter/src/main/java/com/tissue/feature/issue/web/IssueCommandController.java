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
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.BatchOperationResponse;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Issue")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class IssueCommandController {

    private final IssueLifecycleUseCase lifecycleUseCase;
    private final IssueUpdateUseCase updateUseCase;
    private final IssueTransitionUseCase transitionUseCase;
    private final IssueParticipantUseCase participantUseCase;
    private final IssueRelationUseCase relationUseCase;
    private final IssueReviewUseCase reviewUseCase;
    private final IssueTagUseCase tagUseCase;

    @Operation(summary = "Create issue", description = "Create a new issue within a project.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Issue created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Project or issue type not found", content = @Content)
    })
    @PostMapping("projects/{projectKey}/issues")
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

    @Operation(summary = "Batch change parent", description = "Assign a parent issue to multiple issues at once.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Batch operation result returned"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @PatchMapping("projects/{projectKey}/issues/batch/parent")
    public ResponseEntity<BatchOperationResponse> batchChangeParent(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid BatchChangeParentRequest request,
            @CurrentMember MemberDetails memberDetails) {
        BatchOperationResponse response = updateUseCase.batchAssignParent(
                ProjectIdentifier.of(workspaceKey, projectKey), request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Batch remove parent", description = "Remove parent issue from multiple issues at once.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Batch operation result returned"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @DeleteMapping("projects/{projectKey}/issues/batch/parent")
    public ResponseEntity<BatchOperationResponse> batchRemoveParent(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid BatchRemoveParentRequest request,
            @CurrentMember MemberDetails memberDetails) {
        BatchOperationResponse response = updateUseCase.batchRemoveParent(
                ProjectIdentifier.of(workspaceKey, projectKey), request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Batch delete issues", description = "Soft delete multiple issues at once.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Batch operation result returned"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @DeleteMapping("projects/{projectKey}/issues/batch")
    public ResponseEntity<BatchOperationResponse> batchSoftDelete(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid BatchSoftDeleteRequest request,
            @CurrentMember MemberDetails memberDetails) {
        BatchOperationResponse response = lifecycleUseCase.batchSoftDelete(
                ProjectIdentifier.of(workspaceKey, projectKey), request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update common fields", description = "Update common fields of an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @PatchMapping("issues/{issueKey}")
    public ResponseEntity<Void> updateCommonFields(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCommonFieldsRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        updateUseCase.updateCommonFields(
                IssueIdentifier.of(workspaceKey, issueKey), command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update custom fields", description = "Update custom field values of an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Custom fields updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request or field value", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @PatchMapping("issues/{issueKey}/custom")
    public ResponseEntity<Void> updateCustomFields(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCustomFieldsRequest request,
            @CurrentMember MemberDetails memberDetails) {
        updateUseCase.updateCustomFields(
                IssueIdentifier.of(workspaceKey, issueKey), request.customFields(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update story point", description = "Set or update the story point estimate for an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Story point updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @PatchMapping("issues/{issueKey}/storypoint")
    public ResponseEntity<Void> updateStoryPoint(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateStoryPointRequest request,
            @CurrentMember MemberDetails memberDetails) {
        updateUseCase.updateStoryPoint(
                IssueIdentifier.of(workspaceKey, issueKey), request.storyPoint(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Assign parent issue", description = "Assign a parent issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Parent assigned"),
        @ApiResponse(responseCode = "400", description = "Invalid request or circular dependency", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @PutMapping("issues/{issueKey}/parent")
    public ResponseEntity<Void> assignParent(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid AssignParentIssueRequest request,
            @CurrentMember MemberDetails memberDetails) {
        updateUseCase.assignParent(
                IssueIdentifier.of(workspaceKey, issueKey), request.parentIssueKey(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove parent issue", description = "Remove the parent issue assignment.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Parent removed"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @DeleteMapping("issues/{issueKey}/parent")
    public ResponseEntity<Void> removeParent(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        updateUseCase.removeParent(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Perform transition",
            description = "Execute a workflow transition of a issue to change its state.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Transition performed"),
        @ApiResponse(
                responseCode = "400",
                description = "Transition not allowed or guard conditions not met",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue or transition not found", content = @Content)
    })
    @PostMapping("issues/{issueKey}/transitions/{transitionId}")
    public ResponseEntity<Void> performTransition(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid PerformTransitionRequest request,
            @CurrentMember MemberDetails memberDetails) {
        transitionUseCase.performTransition(
                IssueIdentifier.of(workspaceKey, issueKey), request.transitionId(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete issue", description = "Soft-delete an issue. Can be restored later.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue deleted"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @DeleteMapping("issues/{issueKey}")
    public ResponseEntity<Void> softDelete(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        lifecycleUseCase.delete(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restore issue", description = "Restore a soft-deleted issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue restored"),
        @ApiResponse(responseCode = "400", description = "Issue is not deleted", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @PostMapping("issues/{issueKey}/restore")
    public ResponseEntity<Void> restore(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        lifecycleUseCase.restore(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Assign issue", description = "Assign a member to an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Member assigned"),
        @ApiResponse(responseCode = "404", description = "Issue or member not found", content = @Content)
    })
    @PostMapping("issues/{issueKey}/assignees/{memberId}")
    public ResponseEntity<Void> assign(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long memberId,
            @CurrentMember MemberDetails memberDetails) {
        participantUseCase.assign(IssueIdentifier.of(workspaceKey, issueKey), memberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unassign issue", description = "Remove the current assignee from an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Assignee removed"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @DeleteMapping("issues/{issueKey}/assignees")
    public ResponseEntity<Void> unassign(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        participantUseCase.unassign(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Subscribe to issue", description = "Subscribe to an issue to receive notifications.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Subscribed"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @PostMapping("issues/{issueKey}/subscribers")
    public ResponseEntity<Void> subscribe(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        participantUseCase.subscribe(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Unsubscribe from issue",
            description = "Unsubscribe from an issue to stop receiving notifications.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Unsubscribed"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @DeleteMapping("issues/{issueKey}/subscribers")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        participantUseCase.unsubscribe(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add reviewer", description = "Add a reviewer to an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reviewer added"),
        @ApiResponse(responseCode = "404", description = "Issue or member not found", content = @Content)
    })
    @PostMapping("issues/{issueKey}/reviewers/{targetMemberId}")
    public ResponseEntity<Void> addReviewer(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {
        participantUseCase.addReviewer(
                IssueIdentifier.of(workspaceKey, issueKey), targetMemberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove reviewer", description = "Remove a reviewer from an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reviewer removed"),
        @ApiResponse(responseCode = "404", description = "Issue or reviewer not found", content = @Content)
    })
    @DeleteMapping("issues/{issueKey}/reviewers/{targetMemberId}")
    public ResponseEntity<Void> removeReviewer(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {
        participantUseCase.removeReviewer(
                IssueIdentifier.of(workspaceKey, issueKey), targetMemberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add issue relation", description = "Create a relation between two issues.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Relation added"),
        @ApiResponse(responseCode = "400", description = "Invalid relation or self-reference", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Relation already exists", content = @Content)
    })
    @PostMapping("issues/{issueKey}/relations")
    public ResponseEntity<Void> addRelation(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid AddIssueRelationRequest request,
            @CurrentMember MemberDetails memberDetails) {
        relationUseCase.add(
                IssueIdentifier.of(workspaceKey, issueKey),
                request.targetIssueKey(),
                request.relationType(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove issue relation", description = "Remove a relation between two issues.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Relation removed"),
        @ApiResponse(responseCode = "404", description = "Issue or relation not found", content = @Content)
    })
    @DeleteMapping("issues/{issueKey}/relations")
    public ResponseEntity<Void> removeRelation(
            @PathVariable String workspaceKey,
            @PathVariable("issueKey") String sourceIssueKey,
            @RequestBody @Valid RemoveIssueRelationRequest request,
            @CurrentMember MemberDetails memberDetails) {
        relationUseCase.remove(
                IssueIdentifier.of(workspaceKey, sourceIssueKey),
                request.targetIssueKey(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Request review", description = "Request a review from specified reviewers.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Review requested"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @PostMapping("issues/{issueKey}/review")
    public ResponseEntity<Void> requestReview(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid RequestReviewRequest request,
            @CurrentMember MemberDetails memberDetails) {
        reviewUseCase.requestReview(
                IssueIdentifier.of(workspaceKey, issueKey), request.reviewerMemberIds(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Submit review", description = "Submit a review decision (approve or reject).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Review submitted"),
        @ApiResponse(responseCode = "400", description = "Not a reviewer or review not requested", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @PostMapping("issues/{issueKey}/reviews/submit")
    public ResponseEntity<Void> submitReview(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid SubmitReviewRequest request,
            @CurrentMember MemberDetails memberDetails) {
        reviewUseCase.submitReview(
                IssueIdentifier.of(workspaceKey, issueKey), request.approved(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add tag to issue", description = "Attach a tag to an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tag added"),
        @ApiResponse(responseCode = "404", description = "Issue or tag not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Tag already attached", content = @Content)
    })
    @PostMapping("issues/{issueKey}/tags/{tagId}")
    public ResponseEntity<Void> addTag(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long tagId,
            @CurrentMember MemberDetails memberDetails) {
        tagUseCase.addTag(IssueIdentifier.of(workspaceKey, issueKey), tagId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove tag from issue", description = "Remove a tag from an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tag removed"),
        @ApiResponse(responseCode = "404", description = "Issue or tag not found", content = @Content)
    })
    @DeleteMapping("issues/{issueKey}/tags/{tagId}")
    public ResponseEntity<Void> removeTag(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long tagId,
            @CurrentMember MemberDetails memberDetails) {
        tagUseCase.removeTag(IssueIdentifier.of(workspaceKey, issueKey), tagId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
