package com.tissue.api.issue.adapter.in.web;

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

import com.tissue.api.issue.adapter.in.web.dto.request.AddIssueRelationRequest;
import com.tissue.api.issue.adapter.in.web.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.adapter.in.web.dto.request.CreateIssueRequest;
import com.tissue.api.issue.adapter.in.web.dto.request.PerformTransitionRequest;
import com.tissue.api.issue.adapter.in.web.dto.request.UpdateCommonFieldsRequest;
import com.tissue.api.issue.adapter.in.web.dto.request.UpdateCustomFieldsRequest;
import com.tissue.api.issue.adapter.in.web.dto.request.UpdateStoryPointRequest;
import com.tissue.api.issue.application.dto.request.AddReviewerCommand;
import com.tissue.api.issue.application.dto.request.AssignIssueCommand;
import com.tissue.api.issue.application.dto.request.AssignParentCommand;
import com.tissue.api.issue.application.dto.request.ChangeReporterCommand;
import com.tissue.api.issue.application.dto.request.DeleteIssueCommand;
import com.tissue.api.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.api.issue.application.dto.request.RemoveAssigneeCommand;
import com.tissue.api.issue.application.dto.request.RemoveParentCommand;
import com.tissue.api.issue.application.dto.request.RemoveReviewerCommand;
import com.tissue.api.issue.application.dto.request.SubscribeIssueCommand;
import com.tissue.api.issue.application.dto.request.UnsubscribeIssueCommand;
import com.tissue.api.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.api.issue.application.dto.response.IssueCommandResult;
import com.tissue.api.issue.application.dto.response.IssueRelationResult;
import com.tissue.api.issue.application.port.in.IssueCommandUseCase;
import com.tissue.api.issue.application.port.in.IssueParticipantUseCase;
import com.tissue.api.issue.application.port.in.IssueRelationUseCase;
import com.tissue.api.issue.application.port.in.IssueTransitionUseCase;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/issues")
@RequiredArgsConstructor
public class IssueCommandController {

	private final IssueCommandUseCase commandUseCase;
	private final IssueTransitionUseCase transitionUseCase;
	private final IssueParticipantUseCase participantUseCase;
	private final IssueRelationUseCase relationUseCase;

	@PostMapping
	public ResponseEntity<IssueCommandResult> create(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestBody @Valid CreateIssueRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueCommandResult response = commandUseCase.create(
			request.toCommand(workspaceKey, projectKey, userDetails.getMemberId())
		);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(response);
	}

	@PatchMapping("/{issueKey}")
	public ResponseEntity<IssueCommandResult> updateCommonFields(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateCommonFieldsRequest request
	) {
		IssueCommandResult response = commandUseCase.updateCommonFields(
			request.toCommand(workspaceKey, projectKey, issueKey)
		);

		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{issueKey}/custom")
	public ResponseEntity<IssueCommandResult> updateCustomFields(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateCustomFieldsRequest request
	) {
		IssueCommandResult response = commandUseCase.updateCustomFields(
			new UpdateCustomFieldsCommand(workspaceKey, projectKey, issueKey, request.customFields())
		);

		return ResponseEntity.ok(response);
	}

	@PutMapping("/{issueKey}/storypoint")
	public ResponseEntity<IssueCommandResult> updateStoryPoint(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateStoryPointRequest request
	) {
		IssueCommandResult response = commandUseCase.updateStoryPoint(
			new UpdateStoryPointCommand(workspaceKey, projectKey, issueKey, request.storyPoint())
		);

		return ResponseEntity.ok(response);
	}

	@PutMapping("/{issueKey}/parent")
	public ResponseEntity<IssueCommandResult> assignParent(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@RequestBody @Valid AssignParentIssueRequest request
	) {
		IssueCommandResult response = commandUseCase.assignParent(
			AssignParentCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.parentIssueKey(request.parentIssueKey())
				.build()
		);

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{issueKey}/parent")
	public ResponseEntity<IssueCommandResult> removeParent(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey
	) {
		IssueCommandResult response = commandUseCase.removeParent(
			new RemoveParentCommand(workspaceKey, projectKey, issueKey)
		);

		return ResponseEntity.ok(response);
	}

	// TODO: /{issueKey}/transition {transitionId: ?} vs /{issueKey}/transition/{transitionId} 어느게 더 좋을까?
	@PostMapping("/{issueKey}/transition")
	public ResponseEntity<IssueCommandResult> performTransition(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@RequestBody @Valid PerformTransitionRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueCommandResult response = transitionUseCase.performTransition(
			PerformTransitionCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.transitionId(request.transitionId())
				.actorMemberId(userDetails.getMemberId())
				.build()
		);

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{issueKey}")
	public ResponseEntity<IssueCommandResult> softDelete(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey
	) {
		IssueCommandResult response = commandUseCase.softDelete(
			new DeleteIssueCommand(workspaceKey, projectKey, issueKey)
		);

		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{issueKey}/reporters/{memberId}")
	public ResponseEntity<IssueCommandResult> changeReporter(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId
	) {
		IssueCommandResult response = participantUseCase.changeReporter(
			ChangeReporterCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.memberId(memberId)
				.build()
		);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{issueKey}/assignees/{memberId}")
	public ResponseEntity<IssueCommandResult> assign(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId
	) {
		IssueCommandResult response = participantUseCase.assign(
			AssignIssueCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.memberId(memberId)
				.build()
		);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{issueKey}/assignees")
	public ResponseEntity<IssueCommandResult> unassign(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey
	) {
		IssueCommandResult response = participantUseCase.unassign(
			new RemoveAssigneeCommand(
				workspaceKey,
				projectKey,
				issueKey
			)
		);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{issueKey}/subscribers")
	public ResponseEntity<IssueCommandResult> subscribe(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueCommandResult response = participantUseCase.subscribe(
			SubscribeIssueCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.memberId(userDetails.getMemberId())
				.build()
		);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{issueKey}/subscribers")
	public ResponseEntity<IssueCommandResult> unsubscribe(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueCommandResult response = participantUseCase.unsubscribe(
			UnsubscribeIssueCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.memberId(userDetails.getMemberId())
				.build()
		);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{issueKey}/reviewers/{memberId}")
	public ResponseEntity<IssueCommandResult> addReviewer(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId
	) {
		IssueCommandResult response = participantUseCase.addReviewer(
			AddReviewerCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.memberId(memberId)
				.build()
		);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{issueKey}/reviewers/{memberId}")
	public ResponseEntity<IssueCommandResult> removeReviewer(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId
	) {
		IssueCommandResult response = participantUseCase.removeReviewer(
			RemoveReviewerCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.memberId(memberId)
				.build()
		);
		return ResponseEntity.ok(response);
	}

	// TODO: cross-project relation을 허용해야 함
	//  이를 위해서 단순히 "{issueKey}"를 path variable로 사용하는게 아니라 "{projectKey}:{issueKey}"를 사용해야 할까?
	//  {projectKey}:{issueKey}가 path variable로 동작한다면 ":" 기준으로 파싱하는 로직이 필요할듯
	//  URI 경로를 어떻게 설계해야할지 고민이 됨

	// TODO: IssueRelationResult 스키마를 다음으로 변경할까? 그런데 내 설계 철학은 커맨드 작업은 웬만하면 식별자만 반환인데
	//  String workspaceKey
	//  String source issue's projectKey
	//  String sourceIssueKey
	//  String target issue's projectKey
	//  String targetIssueKey
	//  Long issueRelationId
	@PostMapping("/{issueKey}/relations")
	public ResponseEntity<IssueRelationResult> addRelation(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@RequestBody @Valid AddIssueRelationRequest request
	) {
		IssueRelationResult response = relationUseCase.add(request.toCommand(workspaceKey, issueKey));
		return ResponseEntity.ok(response);
	}

	// TODO: cross-project relation을 허용하기 때문에 변경 필요
	//  이를 위해서 단순히 "{issueKey}"를 path variable로 사용하는게 아니라 "{projectKey}:{issueKey}"를 사용해야 할까?
	//  {projectKey}:{issueKey}가 path variable로 동작한다면 ":" 기준으로 파싱하는 로직이 필요할듯
	//  URI 경로를 어떻게 설계해야할지 고민이 됨

	// TODO: Void말고 IssueCommandResult(Issue를 식별하기 위한 식별자들)를 사용하는게 좋을까?
	//  또는 IssueRelationResult를 사용하거나
	@DeleteMapping("/{sourceIssueKey}/relations/{targetIssueKey}")
	public ResponseEntity<Void> removeRelation(
		@PathVariable String workspaceKey,
		@PathVariable String sourceIssueKey,
		@PathVariable String targetIssueKey
	) {
		relationUseCase.remove(workspaceKey, sourceIssueKey, targetIssueKey);
		return ResponseEntity.noContent().build();
	}

	// TODO: requestReview() - @PostMapping("/issues/{issueKey}/review")
	// TODO: batchChangeParent() - @PostMapping("/issues/batch/parent")
	// TODO: batchUpdateStoryPoint() - @PostMapping("/issues/batch/storypoint")
	// TODO: batchSoftDelete() - @DeleteMapping("/issues/batch")
	// TODO: cloneIssue() - @PostMapping("/issues/{issueKey}/clone")
	//  - query parameter를 사용해서 cloneIssueToProject()를 사용할지 여부 정하기. 예) ?to-project=true
}


