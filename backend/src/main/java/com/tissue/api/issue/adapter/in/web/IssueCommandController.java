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
import com.tissue.api.issue.application.dto.request.RemoveIssueRelationCommand;
import com.tissue.api.issue.application.dto.request.RemoveParentCommand;
import com.tissue.api.issue.application.dto.request.RemoveReviewerCommand;
import com.tissue.api.issue.application.dto.request.SubscribeIssueCommand;
import com.tissue.api.issue.application.dto.request.UnsubscribeIssueCommand;
import com.tissue.api.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.api.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.api.issue.application.dto.response.IssueCreateResponse;
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
	public ResponseEntity<IssueCreateResponse> create(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestBody @Valid CreateIssueRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueCreateResponse response = commandUseCase.create(
			request.toCommand(workspaceKey, projectKey, userDetails.getMemberId())
		);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(response);
	}

	@PatchMapping("/{issueKey}")
	public ResponseEntity<IssueCreateResponse> updateCommonFields(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateCommonFieldsRequest request
	) {
		commandUseCase.updateCommonFields(request.toCommand(workspaceKey, projectKey, issueKey));
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{issueKey}/custom")
	public ResponseEntity<IssueCreateResponse> updateCustomFields(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateCustomFieldsRequest request
	) {
		commandUseCase.updateCustomFields(
			new UpdateCustomFieldsCommand(workspaceKey, projectKey, issueKey, request.customFields())
		);

		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{issueKey}/storypoint")
	public ResponseEntity<IssueCreateResponse> updateStoryPoint(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateStoryPointRequest request
	) {
		commandUseCase.updateStoryPoint(
			new UpdateStoryPointCommand(workspaceKey, projectKey, issueKey, request.storyPoint())
		);

		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{issueKey}/parent")
	public ResponseEntity<IssueCreateResponse> assignParent(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@RequestBody @Valid AssignParentIssueRequest request
	) {
		commandUseCase.assignParent(
			AssignParentCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.parentIssueKey(request.parentIssueKey())
				.build()
		);

		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{issueKey}/parent")
	public ResponseEntity<IssueCreateResponse> removeParent(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey
	) {
		commandUseCase.removeParent(new RemoveParentCommand(workspaceKey, projectKey, issueKey));
		return ResponseEntity.noContent().build();
	}

	// TODO: /{issueKey}/transition {transitionId: ?} vs /{issueKey}/transition/{transitionId} 어느게 더 좋을까?
	@PostMapping("/{issueKey}/transition")
	public ResponseEntity<IssueCreateResponse> performTransition(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@RequestBody @Valid PerformTransitionRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		transitionUseCase.performTransition(
			PerformTransitionCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.transitionId(request.transitionId())
				.actorMemberId(userDetails.getMemberId())
				.build()
		);

		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{issueKey}")
	public ResponseEntity<IssueCreateResponse> softDelete(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey
	) {
		commandUseCase.softDelete(new DeleteIssueCommand(workspaceKey, projectKey, issueKey));
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{issueKey}/reporters/{memberId}")
	public ResponseEntity<IssueCreateResponse> changeReporter(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId
	) {
		participantUseCase.changeReporter(
			ChangeReporterCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.memberId(memberId)
				.build()
		);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{issueKey}/assignees/{memberId}")
	public ResponseEntity<IssueCreateResponse> assign(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId
	) {
		participantUseCase.assign(
			AssignIssueCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.memberId(memberId)
				.build()
		);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{issueKey}/assignees")
	public ResponseEntity<IssueCreateResponse> unassign(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey
	) {
		participantUseCase.unassign(new RemoveAssigneeCommand(workspaceKey, projectKey, issueKey));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{issueKey}/subscribers")
	public ResponseEntity<IssueCreateResponse> subscribe(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		participantUseCase.subscribe(
			SubscribeIssueCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.memberId(userDetails.getMemberId())
				.build()
		);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{issueKey}/subscribers")
	public ResponseEntity<IssueCreateResponse> unsubscribe(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		participantUseCase.unsubscribe(
			UnsubscribeIssueCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.memberId(userDetails.getMemberId())
				.build()
		);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{issueKey}/reviewers/{memberId}")
	public ResponseEntity<IssueCreateResponse> addReviewer(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId
	) {
		participantUseCase.addReviewer(
			AddReviewerCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.memberId(memberId)
				.build()
		);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{issueKey}/reviewers/{memberId}")
	public ResponseEntity<IssueCreateResponse> removeReviewer(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId
	) {
		participantUseCase.removeReviewer(
			RemoveReviewerCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueKey(issueKey)
				.memberId(memberId)
				.build()
		);
		return ResponseEntity.noContent().build();
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
	public ResponseEntity<Void> addRelation(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable String issueKey,
		@RequestBody @Valid AddIssueRelationRequest request
	) {
		relationUseCase.add(request.toCommand(workspaceKey, issueKey));
		return ResponseEntity.noContent().build();
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
		relationUseCase.remove(new RemoveIssueRelationCommand(workspaceKey, sourceIssueKey, targetIssueKey));
		return ResponseEntity.noContent().build();
	}

	// TODO: requestReview() - @PostMapping("/issues/{issueKey}/review")
	// TODO: batchChangeParent() - @PostMapping("/issues/batch/parent")
	// TODO: batchUpdateStoryPoint() - @PostMapping("/issues/batch/storypoint")
	// TODO: batchSoftDelete() - @DeleteMapping("/issues/batch")
	// TODO: cloneIssue() - @PostMapping("/issues/{issueKey}/clone")
	//  - query parameter를 사용해서 cloneIssueToProject()를 사용할지 여부 정하기. 예) ?to-project=true
}


