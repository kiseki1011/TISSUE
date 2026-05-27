package com.tissue.feature.issue.web;

import com.tissue.feature.issue.application.dto.response.IssueCreateResponse;
import com.tissue.feature.issue.application.port.usecase.IssueLifecycleUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueParticipantUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueRelationUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueReviewUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueTagUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueTransitionUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueUpdateUseCase;
import com.tissue.feature.issue.domain.exception.IssueErrorCode;
import com.tissue.feature.issue.web.request.AddIssueRelationRequest;
import com.tissue.feature.issue.web.request.AssignParentIssueRequest;
import com.tissue.feature.issue.web.request.BatchChangeParentRequest;
import com.tissue.feature.issue.web.request.BatchDeleteRequest;
import com.tissue.feature.issue.web.request.BatchRemoveParentRequest;
import com.tissue.feature.issue.web.request.CreateIssueRequest;
import com.tissue.feature.issue.web.request.PerformTransitionRequest;
import com.tissue.feature.issue.web.request.RemoveIssueRelationRequest;
import com.tissue.feature.issue.web.request.RequestReviewRequest;
import com.tissue.feature.issue.web.request.SubmitReviewRequest;
import com.tissue.feature.issue.web.request.UpdateCommonFieldsRequest;
import com.tissue.feature.issue.web.request.UpdateCustomFieldsRequest;
import com.tissue.feature.issue.web.request.UpdateStoryPointRequest;
import com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.sprint.domain.exception.SprintErrorCode;
import com.tissue.feature.tag.domain.exception.TagErrorCode;
import com.tissue.feature.workflow.domain.exception.WorkflowErrorCode;
import com.tissue.global.openapi.IssueErrors;
import com.tissue.global.openapi.IssueTypeErrors;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.global.openapi.SprintErrors;
import com.tissue.global.openapi.TagErrors;
import com.tissue.global.openapi.WorkflowErrors;
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

    @Operation(operationId = "createIssue", summary = "Create issue", description = """
                    Create a new issue within a project.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Issue created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.DUE_DATE_MUST_BE_FUTURE,
        IssueErrorCode.STORY_POINT_NOT_ALLOWED,
        IssueErrorCode.PARENT_WORKSPACE_MISMATCH,
        IssueErrorCode.PARENT_PROJECT_MISMATCH,
        IssueErrorCode.ISSUE_SELF_REFERENCE,
        IssueErrorCode.INVALID_PARENT_HIERARCHY,
        IssueErrorCode.CUSTOM_FIELD_REQUIRED,
        IssueErrorCode.UNKNOWN_CUSTOM_FIELD_ID,
        IssueErrorCode.CUSTOM_FIELD_TYPE_MISMATCH,
        IssueErrorCode.UNKNOWN_ENUM_OPTION,
        IssueErrorCode.DECIMAL_INTEGER_PART_TOO_LONG,
        IssueErrorCode.DECIMAL_FRACTION_PART_TOO_LONG,
        IssueErrorCode.INVALID_PERCENTAGE_EXCEPTION,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @IssueTypeErrors({IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND})
    @SprintErrors({SprintErrorCode.SPRINT_NOT_FOUND})
    @PostMapping("projects/{projectKey}/issues")
    public ResponseEntity<IssueCreateResponse> createIssue(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid CreateIssueRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        IssueCreateResponse response = lifecycleUseCase.create(
                ProjectIdentifier.of(workspaceKey, projectKey), command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "batchChangeIssueParent", summary = "Batch change parent", description = """
                    Assign a parent issue to multiple issues at once.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Batch operation result returned"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({IssueErrorCode.ISSUE_NOT_FOUND})
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @PatchMapping("projects/{projectKey}/issues/batch/parent")
    public ResponseEntity<BatchOperationResponse> batchChangeIssueParent(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid BatchChangeParentRequest request,
            @CurrentMember MemberDetails memberDetails) {
        BatchOperationResponse response = updateUseCase.batchAssignParent(
                ProjectIdentifier.of(workspaceKey, projectKey), request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "batchRemoveIssueParent", summary = "Batch remove parent", description = """
                    Remove parent issue from multiple issues at once.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Batch operation result returned"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @DeleteMapping("projects/{projectKey}/issues/batch/parent")
    public ResponseEntity<BatchOperationResponse> batchRemoveIssueParent(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid BatchRemoveParentRequest request,
            @CurrentMember MemberDetails memberDetails) {
        BatchOperationResponse response = updateUseCase.batchRemoveParent(
                ProjectIdentifier.of(workspaceKey, projectKey), request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "batchDeleteIssues", summary = "Batch delete issues", description = """
                Soft delete multiple issues at once.

                **Requirements:**
                - Requires workspace `ADMIN`, project `MANAGER`, or issue author (per issue)""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Batch operation result returned"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @DeleteMapping("projects/{projectKey}/issues/batch")
    public ResponseEntity<BatchOperationResponse> batchDeleteIssues(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid BatchDeleteRequest request,
            @CurrentMember MemberDetails memberDetails) {
        BatchOperationResponse response = lifecycleUseCase.batchDelete(
                ProjectIdentifier.of(workspaceKey, projectKey), request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "updateIssueCommonFields", summary = "Update common fields", description = """
                    Update common fields of an issue. Only provided fields are updated.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.DUE_DATE_MUST_BE_FUTURE,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PatchMapping("issues/{issueKey}")
    public ResponseEntity<Void> updateIssueCommonFields(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCommonFieldsRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        updateUseCase.updateCommonFields(
                IssueIdentifier.of(workspaceKey, issueKey), command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "updateIssueCustomFields", summary = "Update custom fields", description = """
                    Update custom field values of an issue.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Custom fields updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.CUSTOM_FIELD_REQUIRED,
        IssueErrorCode.UNKNOWN_CUSTOM_FIELD_ID,
        IssueErrorCode.CUSTOM_FIELD_TYPE_MISMATCH,
        IssueErrorCode.UNKNOWN_ENUM_OPTION,
        IssueErrorCode.DECIMAL_INTEGER_PART_TOO_LONG,
        IssueErrorCode.DECIMAL_FRACTION_PART_TOO_LONG,
        IssueErrorCode.INVALID_PERCENTAGE_EXCEPTION,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PatchMapping("issues/{issueKey}/custom")
    public ResponseEntity<Void> updateIssueCustomFields(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCustomFieldsRequest request,
            @CurrentMember MemberDetails memberDetails) {
        updateUseCase.updateCustomFields(
                IssueIdentifier.of(workspaceKey, issueKey), request.customFields(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "updateIssueStoryPoint", summary = "Update story point", description = """
                    Set or update the story point estimate for an issue.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Story point updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.STORY_POINT_NOT_ALLOWED,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PatchMapping("issues/{issueKey}/storypoint")
    public ResponseEntity<Void> updateIssueStoryPoint(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateStoryPointRequest request,
            @CurrentMember MemberDetails memberDetails) {
        updateUseCase.updateStoryPoint(
                IssueIdentifier.of(workspaceKey, issueKey), request.storyPoint(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "assignIssueParent", summary = "Assign parent issue", description = """
                    Assign a parent issue.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Parent assigned"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.PARENT_WORKSPACE_MISMATCH,
        IssueErrorCode.PARENT_PROJECT_MISMATCH,
        IssueErrorCode.ISSUE_SELF_REFERENCE,
        IssueErrorCode.INVALID_PARENT_HIERARCHY,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PutMapping("issues/{issueKey}/parent")
    public ResponseEntity<Void> assignIssueParent(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid AssignParentIssueRequest request,
            @CurrentMember MemberDetails memberDetails) {
        updateUseCase.assignParent(
                IssueIdentifier.of(workspaceKey, issueKey), request.parentIssueKey(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "removeIssueParent", summary = "Remove parent issue", description = """
                    Remove the parent issue assignment.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Parent removed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.PARENT_REQUIRED,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @DeleteMapping("issues/{issueKey}/parent")
    public ResponseEntity<Void> removeIssueParent(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        updateUseCase.removeParent(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "performIssueTransition", summary = "Perform transition", description = """
                    Execute a workflow transition of an issue to change its state.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Transition performed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.TRANSITION_SOURCE_STATE_NOT_MATCH,
        IssueErrorCode.REVIEW_INCOMPLETE,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @WorkflowErrors({
        WorkflowErrorCode.WORKFLOW_TRANSITION_NOT_FOUND,
        WorkflowErrorCode.TRANSITION_GUARD_FAILED,
    })
    @PostMapping("issues/{issueKey}:performTransition")
    public ResponseEntity<Void> performIssueTransition(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid PerformTransitionRequest request,
            @CurrentMember MemberDetails memberDetails) {
        transitionUseCase.performTransition(
                IssueIdentifier.of(workspaceKey, issueKey), request.transitionId(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteIssue", summary = "Delete issue", description = """
                Soft-delete an issue. Can be restored later.

                **Requirements:**
                - Requires workspace `ADMIN`, project `MANAGER`, or issue author""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.ISSUE_DELETE_NOT_ALLOWED,
        IssueErrorCode.CANNOT_DELETE_ISSUE_WITH_CHILDREN,
        IssueErrorCode.ISSUE_IN_PROGRESS_DELETION_NOT_ALLOWED,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @DeleteMapping("issues/{issueKey}")
    public ResponseEntity<Void> deleteIssue(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        lifecycleUseCase.delete(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "restoreIssue", summary = "Restore issue", description = """
                Restore a soft-deleted issue.

                **Requirements:**
                - Requires workspace `ADMIN`, project `MANAGER`, or issue author""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue restored"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.ISSUE_DELETE_NOT_ALLOWED,
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @PostMapping("issues/{issueKey}:restore")
    public ResponseEntity<Void> restoreIssue(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        lifecycleUseCase.restore(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "assignIssue", summary = "Assign issue", description = """
                Assign a member to an issue.

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Member assigned"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({IssueErrorCode.ISSUE_NOT_FOUND})
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PutMapping("issues/{issueKey}/assignees/{memberId}")
    public ResponseEntity<Void> assignIssue(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long memberId,
            @CurrentMember MemberDetails memberDetails) {
        participantUseCase.assign(IssueIdentifier.of(workspaceKey, issueKey), memberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "unassignIssue", summary = "Unassign issue", description = """
                    Remove the current assignee from an issue.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Assignee removed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({IssueErrorCode.ISSUE_NOT_FOUND})
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @DeleteMapping("issues/{issueKey}/assignees")
    public ResponseEntity<Void> unassignIssue(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        participantUseCase.unassign(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "subscribeIssue", summary = "Subscribe to issue", description = """
                    Subscribe to an issue to receive notifications.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Subscribed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({IssueErrorCode.ISSUE_NOT_FOUND})
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PostMapping("issues/{issueKey}/subscribers")
    public ResponseEntity<Void> subscribeIssue(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        participantUseCase.subscribe(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "unsubscribeIssue", summary = "Unsubscribe from issue", description = """
                    Unsubscribe from an issue to stop receiving notifications.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Unsubscribed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({IssueErrorCode.ISSUE_NOT_FOUND})
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @DeleteMapping("issues/{issueKey}/subscribers")
    public ResponseEntity<Void> unsubscribeIssue(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        participantUseCase.unsubscribe(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "addIssueReviewer", summary = "Add reviewer", description = """
                Add a reviewer to an issue.

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reviewer added"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.MAX_REVIEWERS_EXCEEDED,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PutMapping("issues/{issueKey}/reviewers/{targetMemberId}")
    public ResponseEntity<Void> addIssueReviewer(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {
        participantUseCase.addReviewer(
                IssueIdentifier.of(workspaceKey, issueKey), targetMemberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "removeIssueReviewer", summary = "Remove reviewer", description = """
                    Remove a reviewer from an issue.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reviewer removed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({IssueErrorCode.ISSUE_NOT_FOUND})
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @DeleteMapping("issues/{issueKey}/reviewers/{targetMemberId}")
    public ResponseEntity<Void> removeIssueReviewer(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {
        participantUseCase.removeReviewer(
                IssueIdentifier.of(workspaceKey, issueKey), targetMemberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "addIssueRelation", summary = "Add issue relation", description = """
                    Create a relation between two issues.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Relation added"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.RELATION_CIRCULAR_DEPENDENCY,
        IssueErrorCode.RELATION_ALREADY_EXISTS,
        IssueErrorCode.RELATION_WORKSPACE_MISMATCH,
        IssueErrorCode.ISSUE_SELF_REFERENCE,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PostMapping("issues/{issueKey}/relations")
    public ResponseEntity<Void> addIssueRelation(
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

    @Operation(operationId = "removeIssueRelation", summary = "Remove issue relation", description = """
                    Remove a relation between two issues.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Relation removed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.RELATION_NOT_FOUND,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @DeleteMapping("issues/{issueKey}/relations")
    public ResponseEntity<Void> removeIssueRelation(
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

    @Operation(operationId = "requestIssueReview", summary = "Request review", description = """
                    Request a review from specified reviewers.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Review requested"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({IssueErrorCode.ISSUE_NOT_FOUND})
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @PostMapping("issues/{issueKey}:requestReview")
    public ResponseEntity<Void> requestIssueReview(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid RequestReviewRequest request,
            @CurrentMember MemberDetails memberDetails) {
        reviewUseCase.requestReview(
                IssueIdentifier.of(workspaceKey, issueKey), request.reviewerMemberIds(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "submitIssueReview", summary = "Submit review", description = """
            Submit a review decision (approve or reject) for an issue.

            **Behavior:**
            - `approved: true` — Sets the reviewer's status to `APPROVED`
            - `approved: false` — Sets the reviewer's status to `CHANGES_REQUESTED`

            **Workflow automation:**
            When rejected, if the current state's outgoing transition has a `REQUIRED_APPROVAL` guard \
            with `auto_transition_on_reject` enabled, the issue automatically performs transition \
            of the specified `reject_transition_name`.

            **`REQUIRED_APPROVAL` guard parameters:**
            - `min_approvals` (number, default: 1) — Minimum number of `APPROVED` reviewers \
            required to pass the guarded transition.
            - `block_on_change_request` (boolean, default: true) — If any reviewer has \
            `CHANGES_REQUESTED` status, the guarded transition is blocked.
            - `auto_transition_on_reject` (boolean, default: false) — Enables automatic \
            state transition when a reviewer rejects.
            - `reject_transition_name` (text, required if auto-reject enabled) — \
            The name of the transition to execute automatically on rejection.

            **Requirements:**
            - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Review submitted"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.REVIEWER_NOT_FOUND,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PostMapping("issues/{issueKey}:submitReview")
    public ResponseEntity<Void> submitIssueReview(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid SubmitReviewRequest request,
            @CurrentMember MemberDetails memberDetails) {
        reviewUseCase.submitReview(
                IssueIdentifier.of(workspaceKey, issueKey), request.approved(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "addTagToIssue", summary = "Add tag to issue", description = """
                Attach a tag to an issue.

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tag added"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({IssueErrorCode.ISSUE_NOT_FOUND})
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @TagErrors({TagErrorCode.TAG_NOT_FOUND})
    @PutMapping("issues/{issueKey}/tags/{tagId}")
    public ResponseEntity<Void> addTagToIssue(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long tagId,
            @CurrentMember MemberDetails memberDetails) {
        tagUseCase.addTag(IssueIdentifier.of(workspaceKey, issueKey), tagId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "removeTagFromIssue", summary = "Remove tag from issue", description = """
                    Remove a tag from an issue.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tag removed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({IssueErrorCode.ISSUE_NOT_FOUND})
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @TagErrors({TagErrorCode.TAG_NOT_FOUND})
    @DeleteMapping("issues/{issueKey}/tags/{tagId}")
    public ResponseEntity<Void> removeTagFromIssue(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long tagId,
            @CurrentMember MemberDetails memberDetails) {
        tagUseCase.removeTag(IssueIdentifier.of(workspaceKey, issueKey), tagId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
