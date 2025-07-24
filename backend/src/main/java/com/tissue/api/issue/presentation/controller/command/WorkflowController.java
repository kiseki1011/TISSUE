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
	 *  - dont allow updating and deleting default workflows!
	 *  - update step(label, description)
	 *  - update transition(label, description, sourceStep, targetStep)
	 *  - delete transition
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

	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// @PatchMapping("/{workflowKey}")
	// public ApiResponse<WorkflowResponse> updateWorkflow(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String workflowKey,
	// 	@CurrentMember MemberUserDetails userDetails,
	// 	@RequestBody @Valid UpdateWorkflowRequest req
	// ) {
	// 	WorkflowResponse res = workflowService.updateWorkflow(req.toCommand(workspaceCode, workflowKey));
	//
	// 	return ApiResponse.ok("Workflow updated.", res);
	// }
	//
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// @DeleteMapping("/{workflowKey}")
	// public ApiResponse<Void> deleteWorkflow(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String workflowKey,
	// 	@CurrentMember MemberUserDetails userDetails
	// ) {
	// 	workflowService.deleteWorkflow(workspaceCode, workflowKey);
	//
	// 	return ApiResponse.okWithNoContent("Workflow deleted.");
	// }
	//
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// @PatchMapping("/{workflowKey}/steps/{stepKey}")
	// public ApiResponse<WorkflowResponse> updateWorkflowStep(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String stepKey,
	// 	@CurrentMember MemberUserDetails userDetails,
	// 	@RequestBody @Valid UpdateWorkflowStepRequest req
	// ) {
	// 	WorkflowResponse res = workflowService.updateWorkflowStep(req.toCommand(workspaceCode, stepKey));
	//
	// 	return ApiResponse.ok("Workflow step updated.", res);
	// }
	//
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// @PatchMapping("/{workflowKey}/transitions/{transitionKey}")
	// public ApiResponse<WorkflowResponse> updateWorkflowTransition(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String transitionKey,
	// 	@CurrentMember MemberUserDetails userDetails,
	// 	@RequestBody @Valid UpdateWorkflowTransitionRequest req
	// ) {
	// 	WorkflowResponse res = workflowService.updateWorkflowTransition(req.toCommand(workspaceCode, transitionKey));
	//
	// 	return ApiResponse.ok("Workflow transition updated.", res);
	// }
	//
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// @DeleteMapping("/{workflowKey}/transitions/{transitionKey}")
	// public ApiResponse<Void> deleteWorkflowTransition(
	// 	@PathVariable String workspaceCode,
	// 	@PathVariable String transitionKey,
	// 	@CurrentMember MemberUserDetails userDetails
	// ) {
	// 	workflowService.deleteWorkflowTransition(workspaceCode, transitionKey);
	//
	// 	return ApiResponse.okWithNoContent("Workflow transition deleted.");
	// }
}
