package com.tissue.api.issue.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.application.service.IssueParticipantService;
import com.tissue.api.issue.presentation.dto.response.IssueResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}")
public class IssueParticipantController {

	private final IssueParticipantService issueParticipantService;

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PatchMapping("/reporters/{memberId}")
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
	@PostMapping("/assignees/{memberId}")
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
	@DeleteMapping("/assignees")
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
	@PostMapping("/subscribers")
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
	@DeleteMapping("/subscribers")
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
	@PostMapping("/reviewers/{memberId}")
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
	@DeleteMapping("/reviewers/{memberId}")
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
}
