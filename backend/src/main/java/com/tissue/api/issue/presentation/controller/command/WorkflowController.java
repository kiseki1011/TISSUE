package com.tissue.api.issue.presentation.controller.command;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.application.service.command.WorkflowService;
import com.tissue.api.issue.presentation.controller.dto.request.CreateWorkflowRequest;
import com.tissue.api.issue.presentation.controller.dto.response.WorkflowResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceCode}/workflows")
public class WorkflowController {

	/**
	 * TODO
	 *  - create workflow
	 *  - add new step and transition to workflow
	 *  - update workflow(update label)
	 *  - delete workflow
	 *  - Dont allow updating and deleting default workflows!
	 */

	private final WorkflowService workflowService;

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<WorkflowResponse> createWorkflow(
		@PathVariable String workspaceCode,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid CreateWorkflowRequest req
	) {
		WorkflowResponse res = workflowService.createWorkflow(req.toCommand(workspaceCode));

		return ApiResponse.created("Workflow created.", res);
	}
}
