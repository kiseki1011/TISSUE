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

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.adapter.in.web.dto.request.AddIssueRelationRequest;
import com.tissue.api.issue.adapter.in.web.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.adapter.in.web.dto.request.CreateIssueRequest;
import com.tissue.api.issue.adapter.in.web.dto.request.PerformTransitionRequest;
import com.tissue.api.issue.adapter.in.web.dto.request.UpdateCommonFieldsRequest;
import com.tissue.api.issue.adapter.in.web.dto.request.UpdateCustomFieldsRequest;
import com.tissue.api.issue.adapter.in.web.dto.request.UpdateStoryPointRequest;
import com.tissue.api.issue.application.dto.response.IssueRelationResult;
import com.tissue.api.issue.application.dto.response.IssueResult;
import com.tissue.api.issue.application.port.in.IssueCommandUseCase;
import com.tissue.api.issue.application.port.in.IssueParticipantUseCase;
import com.tissue.api.issue.application.port.in.IssueRelationUseCase;
import com.tissue.api.issue.application.port.in.IssueTransitionUseCase;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues")
@RequiredArgsConstructor
public class IssueCommandController {

	private final IssueCommandUseCase commandUseCase;
	private final IssueTransitionUseCase transitionUseCase;
	private final IssueParticipantUseCase participantUseCase;
	private final IssueRelationUseCase relationUseCase;

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ResponseEntity<ApiResponse<IssueResult>> create(
		@PathVariable String workspaceKey,
		@RequestBody @Valid CreateIssueRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = commandUseCase.create(request.toCommand(workspaceKey, userDetails.getMemberId()));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Issue created.", response));
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}")
	public ApiResponse<IssueResult> updateCommonFields(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateCommonFieldsRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = commandUseCase.updateCommonFields(request.toCommand(workspaceKey, issueKey));
		return ApiResponse.ok("Issue updated.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}/custom")
	public ApiResponse<IssueResult> updateCustomFields(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateCustomFieldsRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = commandUseCase.updateCustomFields(request.toCommand(workspaceKey, issueKey));
		return ApiResponse.ok("Issue updated.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PutMapping("/{issueKey}/storypoint")
	public ApiResponse<IssueResult> updateStoryPoint(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateStoryPointRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = commandUseCase.updateStoryPoint(request.toCommand(workspaceKey, issueKey));
		return ApiResponse.ok("Issue story point updated.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PutMapping("/{issueKey}/parent")
	public ApiResponse<IssueResult> assignParent(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid AssignParentIssueRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = commandUseCase.assignParent(workspaceKey, issueKey, request.parentIssueKey());
		return ApiResponse.ok("Parent issue assigned.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{issueKey}/parent")
	public ApiResponse<IssueResult> removeParent(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = commandUseCase.removeParent(workspaceKey, issueKey);
		return ApiResponse.ok("Parent issue removed.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/{issueKey}/transition")
	public ApiResponse<IssueResult> performTransition(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid PerformTransitionRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = transitionUseCase.performTransition(
			request.toCommand(workspaceKey, issueKey, userDetails.getMemberId())
		);
		return ApiResponse.created("Issue state transitioned.", response);
	}

	@RoleRequired(role = WorkspaceRole.ADMIN)
	@DeleteMapping("/{issueKey}")
	public ApiResponse<IssueResult> softDelete(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = commandUseCase.softDelete(workspaceKey, issueKey);
		return ApiResponse.ok("Issue deleted(archived).", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}/reporters/{memberId}")
	public ApiResponse<IssueResult> changeReporter(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = participantUseCase.changeReporter(workspaceKey, issueKey, memberId);
		return ApiResponse.ok("Reporter changed.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/{issueKey}/assignees/{memberId}")
	public ApiResponse<IssueResult> assignTo(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = participantUseCase.assignTo(
			workspaceKey,
			issueKey,
			memberId
		);
		return ApiResponse.ok("Assignee added.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{issueKey}/assignees")
	public ApiResponse<IssueResult> unassign(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = participantUseCase.unassign(
			workspaceKey,
			issueKey
		);
		return ApiResponse.ok("Assignee removed.", response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@PostMapping("/{issueKey}/subscribers")
	public ApiResponse<IssueResult> subscribe(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = participantUseCase.subscribe(
			workspaceKey,
			issueKey,
			userDetails.getMemberId()
		);
		return ApiResponse.ok("Subscriber added.", response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@DeleteMapping("/{issueKey}/subscribers")
	public ApiResponse<IssueResult> unsubscribe(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = participantUseCase.unsubscribe(
			workspaceKey,
			issueKey,
			userDetails.getMemberId()
		);
		return ApiResponse.ok("Subscriber removed.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/{issueKey}/reviewers/{memberId}")
	public ApiResponse<IssueResult> addReviewer(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = participantUseCase.addReviewer(
			workspaceKey,
			issueKey,
			memberId
		);
		return ApiResponse.ok("Reviewer added.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{issueKey}/reviewers/{memberId}")
	public ApiResponse<IssueResult> removeReviewer(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResult response = participantUseCase.removeReviewer(
			workspaceKey,
			issueKey,
			memberId
		);
		return ApiResponse.ok("Reviewer removed.", response);
	}

	// TODO: 두 이슈 간 중복되지 않는 한 여러 관계를 맺을 수 있도록 허용.
	//  - DUPLICATES 제외(특수 처리 필요)
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/{issueKey}/relations")
	public ApiResponse<IssueRelationResult> addRelation(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid AddIssueRelationRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueRelationResult response = relationUseCase.add(request.toCommand(workspaceKey, issueKey));
		return ApiResponse.ok("Issue relation created.", response);
	}

	// TODO: 변경 필요. RelationType 중에 뭘 제거할지.
	//  - RemoveRelationRequest에 targetIssueKey, relationType을 넣는 방식으로 설계할까?
	//  - 아니면 targetIssueKey가 아닌 relationId를 넘기도록 해도 괜찮을 것 같다(이게 더 좋을 듯)
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{sourceIssueKey}/relations/{targetIssueKey}")
	public ApiResponse<Void> removeRelation(
		@PathVariable String workspaceKey,
		@PathVariable String sourceIssueKey,
		@PathVariable String targetIssueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		relationUseCase.remove(workspaceKey, sourceIssueKey, targetIssueKey);
		return ApiResponse.okWithNoContent("Issue relation removed.");
	}

	// TODO: requestReview() - @PostMapping("/issues/{issueKey}/review")
	// TODO: batchChangeParent() - @PostMapping("/issues/batch/parent")
	// TODO: batchUpdateStoryPoint() - @PostMapping("/issues/batch/storypoint")
	// TODO: batchSoftDelete() - @DeleteMapping("/issues/batch")
	// TODO: cloneIssue() - @PostMapping("/issues/{issueKey}/clone")
	//  - query parameter를 사용해서 cloneIssueToProject()를 사용할지 여부 정하기. 예) ?to-project=true
}


