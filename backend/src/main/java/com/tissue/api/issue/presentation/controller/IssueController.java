package com.tissue.api.issue.presentation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.application.dto.response.IssueDetailDto;
import com.tissue.api.issue.application.service.IssueQueryService;
import com.tissue.api.issue.application.service.IssueService;
import com.tissue.api.issue.application.service.IssueTransitionService;
import com.tissue.api.issue.presentation.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.presentation.dto.request.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.request.PerformTransitionRequest;
import com.tissue.api.issue.presentation.dto.request.UpdateCommonFieldsRequest;
import com.tissue.api.issue.presentation.dto.request.UpdateCustomFieldsRequest;
import com.tissue.api.issue.presentation.dto.request.UpdateStoryPointRequest;
import com.tissue.api.issue.presentation.dto.response.IssueDetailResponse;
import com.tissue.api.issue.presentation.dto.response.IssueResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workflow.presentation.dto.response.TransitionResponse;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues")
@RequiredArgsConstructor
public class IssueController {

	private final IssueService issueService;
	private final IssueTransitionService transitionService;
	private final IssueQueryService queryService;

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ResponseEntity<ApiResponse<IssueResponse>> create(
		@PathVariable String workspaceKey,
		@RequestBody @Valid CreateIssueRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueService.create(request.toCommand(workspaceKey, userDetails.getMemberId()));
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
		IssueResponse response = issueService.updateCommonFields(request.toCommand(workspaceKey, issueKey));
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
		IssueResponse response = issueService.updateCustomFields(request.toCommand(workspaceKey, issueKey));
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
		IssueResponse response = issueService.updateStoryPoint(request.toCommand(workspaceKey, issueKey));
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
		IssueResponse response = issueService.assignParent(workspaceKey, issueKey, request.parentIssueKey());
		return ApiResponse.ok("Parent issue assigned.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{issueKey}/parent")
	public ApiResponse<IssueResponse> removeParent(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueService.removeParent(workspaceKey, issueKey);
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
		IssueResponse response = transitionService.performTransition(
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
		IssueResponse response = issueService.softDelete(workspaceKey, issueKey);
		return ApiResponse.ok("Issue deleted(archived).", response);
	}

	// TODO: TransitionResponse의 경우 Worflow 애그리거트 패키지의 응답 DTO임
	//  - Issue 애그리거트 전용의 TransitionResponse를 따로 만들어야 할까?
	//  그리고 애플리케이션 계층에서는 TransitionResponse가 아닌 TransitionDetailDto를 반환하도록 만들어야 할까?
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}/transitions")
	public ApiResponse<List<TransitionResponse>> getAvailableTransitions(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid PerformTransitionRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		List<TransitionResponse> response = queryService.getAvailableTransitions(workspaceKey, issueKey);
		return ApiResponse.created("", response);
	}

	// TODO: content의 경우 크기가 클수 있어서 캐싱 정책을 적용해야 하지 않을까?
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}")
	public ApiResponse<IssueDetailResponse> getIssueDetails(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueDetailDto detailDto = queryService.getIssueDetails(workspaceKey, issueKey);
		IssueDetailResponse response = IssueDetailResponse.from(detailDto);
		return ApiResponse.ok("Retrieved issue details.", response);
	}

	// TODO: getDetailedIssue()
	//  - join fetch with Workspace, Sprint, WorkspaceMember (reporter), IssueReviewer(s), IssueSubcriber(s)

	// TODO: getIssueCustomFieldValues(): 이슈의 커스텀 필드와 해당 값들을 조회

	// TODO: isStoryPointUpdatable() (더 좋은 이름있다면 개선)
	//  - 이슈 생성 또는 스토리 포인트 업데이트 시 스토리 포인트 설정 가능 여부

	// TODO: getIssues() 페이징 쿼리 API(프로젝트 단위)
	//  - Issue를 조건별로 검색 가능
	//  - 조건
	//    - IssuePriority, dueAt 기간, startedAt 기간, resolvedAt 기간, IssueRelation(outgoing 기준?), Sprint 번호(예시: "SPRINT-123")
	//    - storyPoint 범위, progress 범위(optional)
	//    - currentState의 category, 프로젝트내 워크플로우의 특정 state 기준(optional)
	//    - 해당 조건들에 대한 오름차순, 내림차순이 가능해야 함
	//    - 특정 enum type 커스텀 필드에 대한 특정 선택지(optional)
	//    - 특정 역할에 따른 이슈 목록 검색도 가능해야 함. 예) ?participantId={memberId}&role=assignee
	//  - 검색어 조건: title > content > summary (우선 순위), issueKey -> 빠른 속도 검색 가능해야하고 fuzzy matching과 디바운싱도 염두

	// TODO: requestReview() - @PostMapping("/issues/{issueKey}/review")
	// TODO: batchChangeParent() - @PostMapping("/issues/batch/parent")
	// TODO: batchUpdateStoryPoint() - @PostMapping("/issues/batch/storypoint")
	// TODO: batchSoftDelete() - @DeleteMapping("/issues/batch")
	// TODO: cloneIssue() - @PostMapping("/issues/{issueKey}/clone")
	//  - query parameter를 사용해서 cloneIssueToProject()를 사용할지 여부 정하기. 예) ?to-project=true
}


