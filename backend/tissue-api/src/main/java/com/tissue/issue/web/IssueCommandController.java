package com.tissue.issue.web;

import com.tissue.issue.application.dto.response.IssueCreateResponse;
import com.tissue.issue.application.port.in.IssueCommandUseCase;
import com.tissue.issue.application.port.in.IssueParticipantUseCase;
import com.tissue.issue.application.port.in.IssueRelationUseCase;
import com.tissue.issue.application.port.in.IssueReviewUseCase;
import com.tissue.issue.application.port.in.IssueTransitionUseCase;
import com.tissue.issue.web.request.AddIssueRelationRequest;
import com.tissue.issue.web.request.AssignParentIssueRequest;
import com.tissue.issue.web.request.CreateIssueRequest;
import com.tissue.issue.web.request.PerformTransitionRequest;
import com.tissue.issue.web.request.RemoveIssueRelationRequest;
import com.tissue.issue.web.request.RequestReviewRequest;
import com.tissue.issue.web.request.SubmitReviewRequest;
import com.tissue.issue.web.request.UpdateCommonFieldsRequest;
import com.tissue.issue.web.request.UpdateCustomFieldsRequest;
import com.tissue.issue.web.request.UpdateStoryPointRequest;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.web.resolver.CurrentProjectMember;
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
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/issues")
@RequiredArgsConstructor
public class IssueCommandController {

    private final IssueCommandUseCase commandUseCase;
    private final IssueTransitionUseCase transitionUseCase;
    private final IssueParticipantUseCase participantUseCase;
    private final IssueRelationUseCase relationUseCase;
    private final IssueReviewUseCase reviewUseCase;

    @PostMapping
    public ResponseEntity<IssueCreateResponse> create(
            @RequestBody @Valid CreateIssueRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand();
        IssueCreateResponse response = commandUseCase.create(command, currentProjectMember);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{issueKey}")
    public ResponseEntity<IssueCreateResponse> updateCommonFields(
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCommonFieldsRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand();
        commandUseCase.updateCommonFields(issueKey, command, currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{issueKey}/custom")
    public ResponseEntity<IssueCreateResponse> updateCustomFields(
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCustomFieldsRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        commandUseCase.updateCustomFields(issueKey, request.customFields(), currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{issueKey}/storypoint")
    public ResponseEntity<IssueCreateResponse> updateStoryPoint(
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateStoryPointRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        commandUseCase.updateStoryPoint(issueKey, request.storyPoint(), currentProjectMember);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{issueKey}/parent")
    public ResponseEntity<IssueCreateResponse> assignParent(
            @PathVariable String issueKey,
            @RequestBody @Valid AssignParentIssueRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        commandUseCase.assignParent(issueKey, request.parentIssueKey(), currentProjectMember);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}/parent")
    public ResponseEntity<IssueCreateResponse> removeParent(
            @PathVariable String issueKey, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        commandUseCase.removeParent(issueKey, currentProjectMember);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/transitions/{transitionId}")
    public ResponseEntity<IssueCreateResponse> performTransition(
            @PathVariable String issueKey,
            @RequestBody @Valid PerformTransitionRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        transitionUseCase.performTransition(issueKey, request.transitionId(), currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}")
    public ResponseEntity<IssueCreateResponse> softDelete(
            @PathVariable String issueKey, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        commandUseCase.delete(issueKey, currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/assignees/{memberId}")
    public ResponseEntity<IssueCreateResponse> assign(
            @PathVariable String issueKey,
            @PathVariable Long memberId,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        participantUseCase.assign(issueKey, memberId, currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}/assignees")
    public ResponseEntity<IssueCreateResponse> unassign(
            @PathVariable String issueKey, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        participantUseCase.unassign(issueKey, currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/subscribers")
    public ResponseEntity<IssueCreateResponse> subscribe(
            @PathVariable String issueKey, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        participantUseCase.subscribe(issueKey, currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}/subscribers")
    public ResponseEntity<IssueCreateResponse> unsubscribe(
            @PathVariable String issueKey, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        participantUseCase.unsubscribe(issueKey, currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/reviewers/{memberId}")
    public ResponseEntity<IssueCreateResponse> addReviewer(
            @PathVariable String issueKey,
            @PathVariable Long memberId,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        participantUseCase.addReviewer(issueKey, memberId, currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}/reviewers/{memberId}")
    public ResponseEntity<IssueCreateResponse> removeReviewer(
            @PathVariable String issueKey,
            @PathVariable Long memberId,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        participantUseCase.removeReviewer(issueKey, memberId, currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/relations")
    public ResponseEntity<Void> addRelation(
            @PathVariable String issueKey,
            @RequestBody @Valid AddIssueRelationRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        relationUseCase.add(
                issueKey,
                request.targetProjectKey(),
                request.targetIssueKey(),
                request.relationType(),
                currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sourceIssueKey}/relations")
    public ResponseEntity<Void> removeRelation(
            @PathVariable String sourceIssueKey,
            @RequestBody @Valid RemoveIssueRelationRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        relationUseCase.remove(
                sourceIssueKey, request.targetProjectKey(), request.targetIssueKey(), currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/review")
    public ResponseEntity<Void> requestReview(
            @PathVariable String issueKey,
            @RequestBody @Valid RequestReviewRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        reviewUseCase.requestReview(issueKey, request.reviewerMemberIds(), currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/reviews/submit")
    public ResponseEntity<Void> submitReview(
            @PathVariable String issueKey,
            @RequestBody @Valid SubmitReviewRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        reviewUseCase.submitReview(issueKey, request.approved(), currentProjectMember);

        return ResponseEntity.noContent().build();
    }
}
