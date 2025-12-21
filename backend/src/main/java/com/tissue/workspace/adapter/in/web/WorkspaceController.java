package com.tissue.workspace.adapter.in.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.resolver.CurrentMember;
import com.tissue.workspace.adapter.in.web.dto.request.CreateWorkspaceRequest;
import com.tissue.workspace.adapter.in.web.dto.request.UpdateWorkspaceInfoRequest;
import com.tissue.workspace.application.dto.request.DeleteWorkspaceCommand;
import com.tissue.workspace.application.dto.request.TransferOwnershipCommand;
import com.tissue.workspace.application.dto.response.WorkspaceCommandResponse;
import com.tissue.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.workspace.application.port.in.WorkspaceCommandUseCase;
import com.tissue.workspace.application.port.in.WorkspaceCreateUseCase;
import com.tissue.workspace.application.port.in.WorkspaceQueryUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

	private final WorkspaceCreateUseCase workspaceCreateUseCase;
	private final WorkspaceCommandUseCase workspaceCommandUseCase;
	private final WorkspaceQueryUseCase workspaceQueryUseCase;

	@PostMapping
	public ResponseEntity<WorkspaceCommandResponse> createWorkspace(
		@RequestBody @Valid CreateWorkspaceRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		var command = request.toCommand(userDetails.getMemberId());
		WorkspaceCommandResponse response = workspaceCreateUseCase.create(command);

		URI location = ServletUriComponentsBuilder
			.fromCurrentRequest()
			.path("/{workspaceKey}")
			.buildAndExpand(response.workspaceKey())
			.toUri();

		return ResponseEntity.created(location)
			.body(response);
	}

	@PatchMapping("/{workspaceKey}")
	public ResponseEntity<Void> updateWorkspaceInfo(
		@PathVariable String workspaceKey,
		@RequestBody @Valid UpdateWorkspaceInfoRequest request
	) {
		var command = request.toCommand(workspaceKey);
		workspaceCommandUseCase.updateInfo(command);

		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{workspaceKey}")
	public ResponseEntity<Void> delete(
		@PathVariable String workspaceKey
	) {
		var command = new DeleteWorkspaceCommand(workspaceKey);
		workspaceCommandUseCase.delete(command);

		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{workspaceKey}/members/{memberId}/ownership")
	public ResponseEntity<Void> transferOwnership(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		var command = new TransferOwnershipCommand(workspaceKey, userDetails.getMemberId(), memberId);
		workspaceCommandUseCase.transferOwnership(command);

		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{workspaceKey}")
	public ResponseEntity<WorkspaceDetail> getWorkspaceDetail(
		@PathVariable String workspaceKey
	) {
		WorkspaceDetail response = workspaceQueryUseCase.getDetail(workspaceKey);
		return ResponseEntity.ok(response);
	}
}
