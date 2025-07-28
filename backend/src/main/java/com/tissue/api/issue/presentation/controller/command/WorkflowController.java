package com.tissue.api.issue.presentation.controller.command;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
	 *  - add new step and transition to workflow
	 *  - update workflow(update label)
	 *  - delete workflow
	 *  - dont allow deleting/updating default workflows(and the steps and transitions inside)!
	 *  - update step(label, description)
	 *  - update transition(label, description, sourceStep, targetStep)
	 *  - needs to apply Spring State Machine
	 *  - set guard for transitions
	 */

	private final WorkflowService workflowService;

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ResponseEntity<ApiResponse<WorkflowResponse>> createWorkflow(
		@PathVariable String workspaceCode,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid CreateWorkflowRequest request
	) {
		WorkflowResponse response = workflowService.createWorkflow(request.toCommand(workspaceCode));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Workflow created.", response));
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
