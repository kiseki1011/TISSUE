package com.tissue.issue.web;

import com.tissue.feature.issue.application.dto.response.IssueCreateResponse;
import com.tissue.feature.issue.application.port.usecase.IssueCommandUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueParticipantUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueRelationUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueReviewUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueTransitionUseCase;
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
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
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
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class IssueCommandController {

    private final IssueCommandUseCase commandUseCase;
    private final IssueTransitionUseCase transitionUseCase;
    private final IssueParticipantUseCase participantUseCase;
    private final IssueRelationUseCase relationUseCase;
    private final IssueReviewUseCase reviewUseCase;

    @PostMapping("/projects/{projectKey}")
    public ResponseEntity<IssueCreateResponse> create(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid CreateIssueRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        IssueCreateResponse response = commandUseCase.create(
                ProjectIdentifier.of(workspaceKey, projectKey), command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/issues/{issueKey}")
    public ResponseEntity<IssueCreateResponse> updateCommonFields(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCommonFieldsRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        commandUseCase.updateCommonFields(
                IssueIdentifier.of(workspaceKey, issueKey), command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/issues/{issueKey}/custom")
    public ResponseEntity<IssueCreateResponse> updateCustomFields(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCustomFieldsRequest request,
            @CurrentMember MemberDetails memberDetails) {

        commandUseCase.updateCustomFields(
                IssueIdentifier.of(workspaceKey, issueKey), request.customFields(), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/issues/{issueKey}/storypoint")
    public ResponseEntity<IssueCreateResponse> updateStoryPoint(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateStoryPointRequest request,
            @CurrentMember MemberDetails memberDetails) {

        commandUseCase.updateStoryPoint(
                IssueIdentifier.of(workspaceKey, issueKey), request.storyPoint(), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/issues/{issueKey}/parent")
    public ResponseEntity<IssueCreateResponse> assignParent(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid AssignParentIssueRequest request,
            @CurrentMember MemberDetails memberDetails) {

        commandUseCase.assignParent(
                IssueIdentifier.of(workspaceKey, issueKey), request.parentIssueKey(), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/{issueKey}/parent")
    public ResponseEntity<IssueCreateResponse> removeParent(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        commandUseCase.removeParent(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/transitions/{transitionId}")
    public ResponseEntity<IssueCreateResponse> performTransition(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid PerformTransitionRequest request,
            @CurrentMember MemberDetails memberDetails) {

        transitionUseCase.performTransition(
                IssueIdentifier.of(workspaceKey, issueKey), request.transitionId(), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/{issueKey}")
    public ResponseEntity<IssueCreateResponse> softDelete(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        commandUseCase.delete(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/assignees/{memberId}")
    public ResponseEntity<IssueCreateResponse> assign(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long memberId,
            @CurrentMember MemberDetails memberDetails) {

        participantUseCase.assign(IssueIdentifier.of(workspaceKey, issueKey), memberId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/{issueKey}/assignees")
    public ResponseEntity<IssueCreateResponse> unassign(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        participantUseCase.unassign(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/subscribers")
    public ResponseEntity<IssueCreateResponse> subscribe(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        participantUseCase.subscribe(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/{issueKey}/subscribers")
    public ResponseEntity<IssueCreateResponse> unsubscribe(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        participantUseCase.unsubscribe(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/reviewers/{targetMemberId}")
    public ResponseEntity<IssueCreateResponse> addReviewer(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {

        participantUseCase.addReviewer(
                IssueIdentifier.of(workspaceKey, issueKey), targetMemberId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/{issueKey}/reviewers/{targetMemberId}")
    public ResponseEntity<IssueCreateResponse> removeReviewer(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {

        participantUseCase.removeReviewer(
                IssueIdentifier.of(workspaceKey, issueKey), targetMemberId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/relations")
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

    @DeleteMapping("/issues/{sourceIssueKey}/relations")
    public ResponseEntity<Void> removeRelation(
            @PathVariable String workspaceKey,
            @PathVariable String sourceIssueKey,
            @RequestBody @Valid RemoveIssueRelationRequest request,
            @CurrentMember MemberDetails memberDetails) {

        relationUseCase.remove(
                IssueIdentifier.of(workspaceKey, sourceIssueKey),
                request.targetIssueKey(),
                memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/review")
    public ResponseEntity<Void> requestReview(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid RequestReviewRequest request,
            @CurrentMember MemberDetails memberDetails) {

        reviewUseCase.requestReview(
                IssueIdentifier.of(workspaceKey, issueKey), request.reviewerMemberIds(), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/issues/{issueKey}/reviews/submit")
    public ResponseEntity<Void> submitReview(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid SubmitReviewRequest request,
            @CurrentMember MemberDetails memberDetails) {

        reviewUseCase.submitReview(
                IssueIdentifier.of(workspaceKey, issueKey), request.approved(), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
