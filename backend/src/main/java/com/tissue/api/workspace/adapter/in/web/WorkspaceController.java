package com.tissue.api.workspace.adapter.in.web;

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

import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.workspace.adapter.in.web.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.adapter.in.web.dto.request.UpdateWorkspaceInfoRequest;
import com.tissue.api.workspace.application.dto.request.DeleteWorkspaceCommand;
import com.tissue.api.workspace.application.dto.request.TransferOwnershipCommand;
import com.tissue.api.workspace.application.dto.response.WorkspaceCommandResponse;
import com.tissue.api.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.api.workspace.application.port.in.WorkspaceCommandUseCase;
import com.tissue.api.workspace.application.port.in.WorkspaceCreateUseCase;
import com.tissue.api.workspace.application.port.in.WorkspaceQueryUseCase;

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
		WorkspaceCommandResponse response = workspaceCreateUseCase.create(
			request.toCommand(userDetails.getMemberId()));

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
		workspaceCommandUseCase.updateInfo(request.toCommand(workspaceKey));
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{workspaceKey}")
	public ResponseEntity<Void> delete(
		@PathVariable String workspaceKey
	) {
		workspaceCommandUseCase.delete(new DeleteWorkspaceCommand(workspaceKey));
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{workspaceKey}/members/{memberId}/ownership")
	public ResponseEntity<Void> transferOwnership(
		@PathVariable String workspaceKey,
		@PathVariable Long memberId,
		@CurrentMember MemberUserDetails userDetails
	) {
		workspaceCommandUseCase.transferOwnership(
			new TransferOwnershipCommand(
				workspaceKey,
				userDetails.getMemberId(),
				memberId
			)
		);
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
