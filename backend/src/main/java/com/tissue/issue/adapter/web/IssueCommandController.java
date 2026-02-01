package com.tissue.issue.adapter.web;

import com.tissue.issue.adapter.web.request.AddIssueRelationRequest;
import com.tissue.issue.adapter.web.request.AssignParentIssueRequest;
import com.tissue.issue.adapter.web.request.CreateIssueRequest;
import com.tissue.issue.adapter.web.request.PerformTransitionRequest;
import com.tissue.issue.adapter.web.request.RemoveIssueRelationRequest;
import com.tissue.issue.adapter.web.request.RequestReviewRequest;
import com.tissue.issue.adapter.web.request.SubmitReviewRequest;
import com.tissue.issue.adapter.web.request.UpdateCommonFieldsRequest;
import com.tissue.issue.adapter.web.request.UpdateCustomFieldsRequest;
import com.tissue.issue.adapter.web.request.UpdateStoryPointRequest;
import com.tissue.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.issue.application.dto.request.AddReviewerCommand;
import com.tissue.issue.application.dto.request.AssignIssueCommand;
import com.tissue.issue.application.dto.request.ChangeReporterCommand;
import com.tissue.issue.application.dto.request.DeleteIssueCommand;
import com.tissue.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.issue.application.dto.request.RemoveAssigneeCommand;
import com.tissue.issue.application.dto.request.RemoveParentCommand;
import com.tissue.issue.application.dto.request.RemoveReviewerCommand;
import com.tissue.issue.application.dto.request.RequestReviewCommand;
import com.tissue.issue.application.dto.request.SubmitReviewCommand;
import com.tissue.issue.application.dto.request.SubscribeIssueCommand;
import com.tissue.issue.application.dto.request.UnsubscribeIssueCommand;
import com.tissue.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.issue.application.dto.response.IssueCreateResponse;
import com.tissue.issue.application.port.in.IssueCommandUseCase;
import com.tissue.issue.application.port.in.IssueParticipantUseCase;
import com.tissue.issue.application.port.in.IssueRelationUseCase;
import com.tissue.issue.application.port.in.IssueReviewUseCase;
import com.tissue.issue.application.port.in.IssueTransitionUseCase;
import com.tissue.project.adapter.web.resolver.CurrentProjectMember;
import com.tissue.project.application.dto.ProjectMemberContext;
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

        var command = request.toCommand(currentProjectMember);
        IssueCreateResponse response = commandUseCase.create(command);

        // TODO: use created

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{issueKey}")
    public ResponseEntity<IssueCreateResponse> updateCommonFields(
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCommonFieldsRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand(issueKey, currentProjectMember);
        commandUseCase.updateCommonFields(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{issueKey}/custom")
    public ResponseEntity<IssueCreateResponse> updateCustomFields(
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCustomFieldsRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new UpdateCustomFieldsCommand(issueKey, request.customFields(), currentProjectMember);
        commandUseCase.updateCustomFields(command);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{issueKey}/storypoint")
    public ResponseEntity<IssueCreateResponse> updateStoryPoint(
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateStoryPointRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new UpdateStoryPointCommand(issueKey, request.storyPoint(), currentProjectMember);
        commandUseCase.updateStoryPoint(command);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{issueKey}/parent")
    public ResponseEntity<IssueCreateResponse> assignParent(
            @PathVariable String issueKey,
            @RequestBody @Valid AssignParentIssueRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand(issueKey, currentProjectMember);
        commandUseCase.assignParent(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}/parent")
    public ResponseEntity<IssueCreateResponse> removeParent(
            @PathVariable String issueKey, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new RemoveParentCommand(issueKey, currentProjectMember);
        commandUseCase.removeParent(command);

        return ResponseEntity.noContent().build();
    }

    // TODO: Which design is better?
    //  /{issueKey}/transition {transitionId: ?} vs /{issueKey}/transition/{transitionId}
    @PostMapping("/{issueKey}/transitions/{transitionId}")
    public ResponseEntity<IssueCreateResponse> performTransition(
            @PathVariable String issueKey,
            @RequestBody @Valid PerformTransitionRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new PerformTransitionCommand(issueKey, request.transitionId(), currentProjectMember);
        transitionUseCase.performTransition(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}")
    public ResponseEntity<IssueCreateResponse> softDelete(
            @PathVariable String issueKey, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new DeleteIssueCommand(issueKey, currentProjectMember);
        commandUseCase.softDelete(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{issueKey}/reporters/{memberId}")
    public ResponseEntity<IssueCreateResponse> changeReporter(
            @PathVariable String issueKey,
            @PathVariable Long memberId,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new ChangeReporterCommand(issueKey, memberId, currentProjectMember);
        participantUseCase.changeReporter(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/assignees/{memberId}")
    public ResponseEntity<IssueCreateResponse> assign(
            @PathVariable String issueKey,
            @PathVariable Long memberId,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new AssignIssueCommand(issueKey, memberId, currentProjectMember);
        participantUseCase.assign(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}/assignees")
    public ResponseEntity<IssueCreateResponse> unassign(
            @PathVariable String issueKey, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new RemoveAssigneeCommand(issueKey, currentProjectMember);
        participantUseCase.unassign(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/subscribers")
    public ResponseEntity<IssueCreateResponse> subscribe(
            @PathVariable String issueKey, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new SubscribeIssueCommand(issueKey, currentProjectMember);
        participantUseCase.subscribe(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}/subscribers")
    public ResponseEntity<IssueCreateResponse> unsubscribe(
            @PathVariable String issueKey, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new UnsubscribeIssueCommand(issueKey, currentProjectMember);
        participantUseCase.unsubscribe(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/reviewers/{memberId}")
    public ResponseEntity<IssueCreateResponse> addReviewer(
            @PathVariable String issueKey,
            @PathVariable Long memberId,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new AddReviewerCommand(issueKey, memberId, currentProjectMember);
        participantUseCase.addReviewer(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}/reviewers/{memberId}")
    public ResponseEntity<IssueCreateResponse> removeReviewer(
            @PathVariable String issueKey,
            @PathVariable Long memberId,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new RemoveReviewerCommand(issueKey, memberId, currentProjectMember);
        participantUseCase.removeReviewer(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/relations")
    public ResponseEntity<Void> addRelation(
            @PathVariable String issueKey,
            @RequestBody @Valid AddIssueRelationRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        AddIssueRelationCommand command = request.toCommand(issueKey, currentProjectMember);
        relationUseCase.add(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sourceIssueKey}/relations")
    public ResponseEntity<Void> removeRelation(
            @PathVariable String sourceIssueKey,
            @RequestBody @Valid RemoveIssueRelationRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand(sourceIssueKey, currentProjectMember);
        relationUseCase.remove(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/review")
    public ResponseEntity<Void> requestReview(
            @PathVariable String issueKey,
            @RequestBody @Valid RequestReviewRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new RequestReviewCommand(issueKey, request.reviewerMemberIds(), currentProjectMember);
        reviewUseCase.requestReview(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/reviews/submit")
    public ResponseEntity<Void> submitReview(
            @PathVariable String issueKey,
            @RequestBody @Valid SubmitReviewRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new SubmitReviewCommand(issueKey, request.approved(), currentProjectMember);
        reviewUseCase.submitReview(command);

        return ResponseEntity.noContent().build();
    }

    // TODO: batchChangeParent() - @PostMapping("/issues/batch/parent")
    // TODO: batchSoftDelete() - @DeleteMapping("/issues/batch")
    // TODO: cloneIssue() - @PostMapping("/issues/{issueKey}/clone")
    //  - query parameter를 사용해서 cloneIssueToProject()를 사용할지 여부 정하기. 예) ?to-project=true
}
