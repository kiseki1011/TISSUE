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
import com.tissue.api.issue.adapter.in.web.dto.response.IssueRelationResponse;
import com.tissue.api.issue.adapter.in.web.dto.response.IssueResponse;
import com.tissue.api.issue.application.service.IssueCommandService;
import com.tissue.api.issue.application.service.IssueParticipantService;
import com.tissue.api.issue.application.service.IssueRelationService;
import com.tissue.api.issue.application.service.IssueTransitionService;
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

	private final IssueCommandService issueCommandService;
	private final IssueTransitionService issueTransitionService;
	private final IssueParticipantService issueParticipantService;
	private final IssueRelationService issueRelationService;

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ResponseEntity<ApiResponse<IssueResponse>> create(
		@PathVariable String workspaceKey,
		@RequestBody @Valid CreateIssueRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCommandService.create(request.toCommand(workspaceKey, userDetails.getMemberId()));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Issue created.", response));
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}")
	public ApiResponse<IssueResponse> updateCommonFields(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateCommonFieldsRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCommandService.updateCommonFields(request.toCommand(workspaceKey, issueKey));
		return ApiResponse.ok("Issue updated.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}/custom")
	public ApiResponse<IssueResponse> updateCustomFields(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateCustomFieldsRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCommandService.updateCustomFields(request.toCommand(workspaceKey, issueKey));
		return ApiResponse.ok("Issue updated.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PutMapping("/{issueKey}/storypoint")
	public ApiResponse<IssueResponse> updateStoryPoint(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateStoryPointRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCommandService.updateStoryPoint(request.toCommand(workspaceKey, issueKey));
		return ApiResponse.ok("Issue story point updated.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PutMapping("/{issueKey}/parent")
	public ApiResponse<IssueResponse> assignParent(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid AssignParentIssueRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCommandService.assignParent(workspaceKey, issueKey, request.parentIssueKey());
		return ApiResponse.ok("Parent issue assigned.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{issueKey}/parent")
	public ApiResponse<IssueResponse> removeParent(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCommandService.removeParent(workspaceKey, issueKey);
		return ApiResponse.ok("Parent issue removed.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/{issueKey}/transition")
	public ApiResponse<IssueResponse> performTransition(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid PerformTransitionRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueTransitionService.performTransition(
			request.toCommand(workspaceKey, issueKey, userDetails.getMemberId())
		);
		return ApiResponse.created("Issue state transitioned.", response);
	}

	@RoleRequired(role = WorkspaceRole.ADMIN)
	@DeleteMapping("/{issueKey}")
	public ApiResponse<IssueResponse> softDelete(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCommandService.softDelete(workspaceKey, issueKey);
		return ApiResponse.ok("Issue deleted(archived).", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}/reporters/{memberId}")
	public ApiResponse<IssueResponse> changeReporter(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueParticipantService.changeReporter(workspaceKey, issueKey, memberId);
		return ApiResponse.ok("Reporter changed.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/{issueKey}/assignees/{memberId}")
	public ApiResponse<IssueResponse> assignTo(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueParticipantService.assignTo(
			workspaceKey,
			issueKey,
			memberId
		);
		return ApiResponse.ok("Assignee added.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{issueKey}/assignees")
	public ApiResponse<IssueResponse> unassign(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueParticipantService.unassign(
			workspaceKey,
			issueKey
		);
		return ApiResponse.ok("Assignee removed.", response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@PostMapping("/{issueKey}/subscribers")
	public ApiResponse<IssueResponse> subscribe(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueParticipantService.subscribe(
			workspaceKey,
			issueKey,
			userDetails.getMemberId()
		);
		return ApiResponse.ok("Subscriber added.", response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@DeleteMapping("/{issueKey}/subscribers")
	public ApiResponse<IssueResponse> unsubscribe(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueParticipantService.unsubscribe(
			workspaceKey,
			issueKey,
			userDetails.getMemberId()
		);
		return ApiResponse.ok("Subscriber removed.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/{issueKey}/reviewers/{memberId}")
	public ApiResponse<IssueResponse> addReviewer(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueParticipantService.addReviewer(
			workspaceKey,
			issueKey,
			memberId
		);
		return ApiResponse.ok("Reviewer added.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{issueKey}/reviewers/{memberId}")
	public ApiResponse<IssueResponse> removeReviewer(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueParticipantService.removeReviewer(
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
	public ApiResponse<IssueRelationResponse> addRelation(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid AddIssueRelationRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueRelationResponse response = issueRelationService.add(request.toCommand(workspaceKey, issueKey));

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
		issueRelationService.remove(workspaceKey, sourceIssueKey, targetIssueKey);

		return ApiResponse.okWithNoContent("Issue relation removed.");
	}

	// TODO: requestReview() - @PostMapping("/issues/{issueKey}/review")
	// TODO: batchChangeParent() - @PostMapping("/issues/batch/parent")
	// TODO: batchUpdateStoryPoint() - @PostMapping("/issues/batch/storypoint")
	// TODO: batchSoftDelete() - @DeleteMapping("/issues/batch")
	// TODO: cloneIssue() - @PostMapping("/issues/{issueKey}/clone")
	//  - query parameter를 사용해서 cloneIssueToProject()를 사용할지 여부 정하기. 예) ?to-project=true
}


