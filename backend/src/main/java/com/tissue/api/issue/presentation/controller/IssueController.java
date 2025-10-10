package com.tissue.api.issue.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.application.service.IssueService;
import com.tissue.api.issue.presentation.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.presentation.dto.request.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.request.UpdateCommonFieldsRequest;
import com.tissue.api.issue.presentation.dto.request.UpdateCustomFieldsRequest;
import com.tissue.api.issue.presentation.dto.response.IssueResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues")
public class IssueController {

	private final IssueService issueService;

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
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateCommonFieldsRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueService.updateCommonFields(request.toCommand(workspaceCode, issueKey));
		return ApiResponse.ok("Issue updated.", response);
	}

	// TODO: 커스텀 필드 업데이트에 대한 경로는 "/{issueKey}/custom"가 괜찮을까?
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}/custom")
	public ApiResponse<IssueResponse> updateCustomFields(
		@PathVariable String workspaceCode,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateCustomFieldsRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueService.updateCustomFields(request.toCommand(workspaceCode, issueKey));
		return ApiResponse.ok("Issue updated.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/{issueKey}/parent")
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

	// TODO: progressWorkflow, 이슈의 IssueType에 따른 Workflow를 따라 상태 전이를 골라서 진행
	//  - 이걸 IssueTransitionController로 분리할까?

	// TODO: Soft delete Issue

	// TODO(Later):
	//  - Clone Issue
	//  - Move(or clone) Issue to different Project
}
