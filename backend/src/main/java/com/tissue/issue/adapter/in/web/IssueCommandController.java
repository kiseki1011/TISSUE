package com.tissue.issue.adapter.in.web;

import com.tissue.issue.adapter.in.web.dto.request.AddIssueRelationRequest;
import com.tissue.issue.adapter.in.web.dto.request.AssignParentIssueRequest;
import com.tissue.issue.adapter.in.web.dto.request.CreateIssueRequest;
import com.tissue.issue.adapter.in.web.dto.request.PerformTransitionRequest;
import com.tissue.issue.adapter.in.web.dto.request.RemoveIssueRelationRequest;
import com.tissue.issue.adapter.in.web.dto.request.RequestReviewRequest;
import com.tissue.issue.adapter.in.web.dto.request.SubmitReviewRequest;
import com.tissue.issue.adapter.in.web.dto.request.UpdateCommonFieldsRequest;
import com.tissue.issue.adapter.in.web.dto.request.UpdateCustomFieldsRequest;
import com.tissue.issue.adapter.in.web.dto.request.UpdateStoryPointRequest;
import com.tissue.issue.application.dto.request.AddIssueRelationCommand;
import com.tissue.issue.application.dto.request.AddReviewerCommand;
import com.tissue.issue.application.dto.request.AssignIssueCommand;
import com.tissue.issue.application.dto.request.AssignParentCommand;
import com.tissue.issue.application.dto.request.ChangeReporterCommand;
import com.tissue.issue.application.dto.request.DeleteIssueCommand;
import com.tissue.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.issue.application.dto.request.RemoveAssigneeCommand;
import com.tissue.issue.application.dto.request.RemoveIssueRelationCommand;
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
import com.tissue.security.authentication.domain.MemberDetails;
import com.tissue.security.authentication.presentation.annotation.CurrentMember;
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
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid CreateIssueRequest request) {

        var command = request.toCommand(workspaceKey, projectKey);
        IssueCreateResponse response = commandUseCase.create(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{issueKey}")
    public ResponseEntity<IssueCreateResponse> updateCommonFields(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCommonFieldsRequest request) {

        var command = request.toCommand(workspaceKey, projectKey, issueKey);
        commandUseCase.updateCommonFields(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{issueKey}/custom")
    public ResponseEntity<IssueCreateResponse> updateCustomFields(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCustomFieldsRequest request) {

        var command = new UpdateCustomFieldsCommand(workspaceKey, projectKey, issueKey, request.customFields());
        commandUseCase.updateCustomFields(command);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{issueKey}/storypoint")
    public ResponseEntity<IssueCreateResponse> updateStoryPoint(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateStoryPointRequest request) {

        var command = new UpdateStoryPointCommand(workspaceKey, projectKey, issueKey, request.storyPoint());
        commandUseCase.updateStoryPoint(command);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{issueKey}/parent")
    public ResponseEntity<IssueCreateResponse> assignParent(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid AssignParentIssueRequest request) {

        var command = new AssignParentCommand(
                workspaceKey, projectKey, issueKey, request.parentProjectKey(), request.parentIssueKey());
        commandUseCase.assignParent(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}/parent")
    public ResponseEntity<IssueCreateResponse> removeParent(
            @PathVariable String workspaceKey, @PathVariable String projectKey, @PathVariable String issueKey) {

        var command = new RemoveParentCommand(workspaceKey, projectKey, issueKey);
        commandUseCase.removeParent(command);

        return ResponseEntity.noContent().build();
    }

    // TODO: /{issueKey}/transition {transitionId: ?} vs /{issueKey}/transition/{transitionId} 어느게 더
    // 좋을까?
    @PostMapping("/{issueKey}/transition")
    public ResponseEntity<IssueCreateResponse> performTransition(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid PerformTransitionRequest request) {

        var command = PerformTransitionCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueKey(issueKey)
                .transitionId(request.transitionId())
                .build();
        transitionUseCase.performTransition(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}")
    public ResponseEntity<IssueCreateResponse> softDelete(
            @PathVariable String workspaceKey, @PathVariable String projectKey, @PathVariable String issueKey) {

        var command = new DeleteIssueCommand(workspaceKey, projectKey, issueKey);
        commandUseCase.softDelete(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{issueKey}/reporters/{memberId}")
    public ResponseEntity<IssueCreateResponse> changeReporter(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @PathVariable Long memberId) {

        var command = ChangeReporterCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueKey(issueKey)
                .targetMemberId(memberId)
                .build();
        participantUseCase.changeReporter(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/assignees/{memberId}")
    public ResponseEntity<IssueCreateResponse> assign(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @PathVariable Long memberId) {

        var command = AssignIssueCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueKey(issueKey)
                .targetMemberId(memberId)
                .build();
        participantUseCase.assign(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}/assignees")
    public ResponseEntity<IssueCreateResponse> unassign(
            @PathVariable String workspaceKey, @PathVariable String projectKey, @PathVariable String issueKey) {

        var command = new RemoveAssigneeCommand(workspaceKey, projectKey, issueKey);
        participantUseCase.unassign(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/subscribers")
    public ResponseEntity<IssueCreateResponse> subscribe(
            @PathVariable String workspaceKey, @PathVariable String projectKey, @PathVariable String issueKey) {

        var command = SubscribeIssueCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueKey(issueKey)
                .build();
        participantUseCase.subscribe(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}/subscribers")
    public ResponseEntity<IssueCreateResponse> unsubscribe(
            @PathVariable String workspaceKey, @PathVariable String projectKey, @PathVariable String issueKey) {

        var command = UnsubscribeIssueCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueKey(issueKey)
                .build();
        participantUseCase.unsubscribe(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/reviewers/{memberId}")
    public ResponseEntity<IssueCreateResponse> addReviewer(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @PathVariable Long memberId) {

        var command = AddReviewerCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueKey(issueKey)
                .targetMemberId(memberId)
                .build();
        participantUseCase.addReviewer(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueKey}/reviewers/{memberId}")
    public ResponseEntity<IssueCreateResponse> removeReviewer(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @PathVariable Long memberId) {

        var command = RemoveReviewerCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueKey(issueKey)
                .targetMemberId(memberId)
                .build();
        participantUseCase.removeReviewer(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/relations")
    public ResponseEntity<Void> addRelation(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid AddIssueRelationRequest request) {

        AddIssueRelationCommand command = request.toCommand(workspaceKey, projectKey, issueKey);
        relationUseCase.add(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sourceIssueKey}/relations")
    public ResponseEntity<Void> removeRelation(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String sourceIssueKey,
            @RequestBody @Valid RemoveIssueRelationRequest request) {

        var command = new RemoveIssueRelationCommand(
                workspaceKey, projectKey, sourceIssueKey, request.targetProjectKey(), request.targetIssueKey());

        relationUseCase.remove(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/review")
    public ResponseEntity<Void> requestReview(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid RequestReviewRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = RequestReviewCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueKey(issueKey)
                .actorMemberId(memberDetails.getMemberId())
                .reviewerMemberIds(request.reviewerMemberIds())
                .build();
        reviewUseCase.requestReview(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueKey}/reviews/submit")
    public ResponseEntity<Void> submitReview(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid SubmitReviewRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = SubmitReviewCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueKey(issueKey)
                .approved(request.approved())
                .actorMemberId(memberDetails.getMemberId())
                .build();
        reviewUseCase.submitReview(command);

        return ResponseEntity.noContent().build();
    }

    // TODO: batchChangeParent() - @PostMapping("/issues/batch/parent")
    // TODO: batchUpdateStoryPoint() - @PostMapping("/issues/batch/storypoint")
    // TODO: batchSoftDelete() - @DeleteMapping("/issues/batch")
    // TODO: cloneIssue() - @PostMapping("/issues/{issueKey}/clone")
    //  - query parameter를 사용해서 cloneIssueToProject()를 사용할지 여부 정하기. 예) ?to-project=true
}
