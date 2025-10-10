package com.tissue.api.issue.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.application.service.IssueCollaboratorService;
import com.tissue.api.issue.presentation.dto.request.AddAssigneeRequest;
import com.tissue.api.issue.presentation.dto.request.AddReviewerRequest;
import com.tissue.api.issue.presentation.dto.request.RemoveAssigneeRequest;
import com.tissue.api.issue.presentation.dto.request.RemoveReviewerRequest;
import com.tissue.api.issue.presentation.dto.response.IssueResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}")
public class IssueCollaboratorController {

	private final IssueCollaboratorService issueCollaboratorService;

	// TODO: 굳이 request dto를 통해 대상 memberId를 넘겨야 하나?
	//  그냥 "/assignees/{memberId}" 처럼 path variable로 사용하면 안되나?

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/assignees")
	public ApiResponse<IssueResponse> addAssignee(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid AddAssigneeRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCollaboratorService.addAssignee(
			workspaceKey,
			issueKey,
			request.memberId());

		return ApiResponse.ok("Assignee added.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/assignees")
	public ApiResponse<IssueResponse> removeAssignee(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid RemoveAssigneeRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCollaboratorService.removeAssignee(
			workspaceKey,
			issueKey,
			request.memberId()
		);

		return ApiResponse.ok("Assignee removed.", response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@PostMapping("/subscribe")
	public ApiResponse<IssueResponse> subscribeIssue(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCollaboratorService.subscribe(
			workspaceKey,
			issueKey,
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Subscriber added.", response); // TODO: "Subscribed issue."로 바꿀까?
	}

	// TODO: Delete mapping을 사용해도 어차피 동사니깐 unsubscribe로 경로를 바꿀까?
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@DeleteMapping("/subscribe")
	public ApiResponse<IssueResponse> unsubscribeIssue(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCollaboratorService.cancelSubscription(
			workspaceKey,
			issueKey,
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Subscriber removed.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/reviewers")
	public ApiResponse<IssueResponse> addReviewer(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid AddReviewerRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCollaboratorService.addReviewer(
			workspaceKey,
			issueKey,
			request.memberId()
		);

		return ApiResponse.ok("Reviewer added.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/reviewers")
	public ApiResponse<IssueResponse> removeReviewer(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid RemoveReviewerRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueResponse response = issueCollaboratorService.removeReviewer(
			workspaceKey,
			issueKey,
			request.memberId()
		);

		return ApiResponse.ok("Reviewer removed.", response);
	}
}
