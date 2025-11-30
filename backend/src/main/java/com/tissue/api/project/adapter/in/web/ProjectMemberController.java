package com.tissue.api.project.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.project.adapter.in.web.dto.request.AddProjectMembersRequest;
import com.tissue.api.project.adapter.in.web.dto.request.ChangeProjectRoleRequest;
import com.tissue.api.project.application.dto.request.ChangeProjectRoleCommand;
import com.tissue.api.project.application.dto.request.JoinProjectCommand;
import com.tissue.api.project.application.dto.request.KickProjectMemberCommand;
import com.tissue.api.project.application.dto.request.LeaveProjectCommand;
import com.tissue.api.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.api.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.api.project.application.port.in.ProjectMemberCommandUseCase;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/members")
public class ProjectMemberController {

	private final ProjectMemberCommandUseCase commandUseCase;

	@PostMapping("/batch")
	public ResponseEntity<ProjectMembersCommandResult> addMembers(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestBody @Valid AddProjectMembersRequest request
	) {
		ProjectMembersCommandResult response = commandUseCase.addMembers(request.toCommand(workspaceKey, projectKey));

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(response);
	}

	@PatchMapping
	public ResponseEntity<ProjectMemberCommandResult> joinProject(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@CurrentMember MemberUserDetails currentMember
	) {
		ProjectMemberCommandResult response = commandUseCase.join(
			new JoinProjectCommand(workspaceKey, projectKey, currentMember.getMemberId())
		);

		return ResponseEntity.ok(response);
	}

	@DeleteMapping
	public ResponseEntity<ProjectMemberCommandResult> leaveProject(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@CurrentMember MemberUserDetails currentMember
	) {
		ProjectMemberCommandResult response = commandUseCase.leave(
			new LeaveProjectCommand(workspaceKey, projectKey, currentMember.getMemberId())
		);

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{memberId}")
	public ResponseEntity<ProjectMemberCommandResult> kickMember(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails currentMember
	) {
		ProjectMemberCommandResult response = commandUseCase.kickMember(
			KickProjectMemberCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.targetMemberId(memberId)
				.actorMemberId(currentMember.getMemberId())
				.build()
		);

		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{memberId}/role")
	public ResponseEntity<ProjectMemberCommandResult> changeProjectRole(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long memberId,
		@RequestBody @Valid ChangeProjectRoleRequest request,
		@CurrentMember MemberUserDetails currentMember
	) {
		ProjectMemberCommandResult response = commandUseCase.changeProjectRole(
			ChangeProjectRoleCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.newRole(request.newProjectRole())
				.targetMemberId(memberId)
				.actorMemberId(currentMember.getMemberId())
				.build()
		);

		return ResponseEntity.ok(response);
	}

}
