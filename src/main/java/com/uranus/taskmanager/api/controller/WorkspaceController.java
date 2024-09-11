package com.uranus.taskmanager.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.response.WorkspaceResponse;
import com.uranus.taskmanager.api.service.WorkspaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class WorkspaceController {

	private final WorkspaceService workspaceService;

	@PostMapping("/workspaces")
	public ResponseEntity<WorkspaceResponse> createWorkspace(@RequestBody @Valid WorkspaceCreateRequest request) {
		WorkspaceResponse response = workspaceService.create(request);
		log.info("[WorkspaceController.createWorkspace] response = {}", response);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/workspaces/{workspaceId}")
	public ResponseEntity<WorkspaceResponse> getWorkspace(@PathVariable String workspaceId) {
		WorkspaceResponse response = workspaceService.get(workspaceId);
		log.info("[WorkspaceController.getWorkspace] response = {}", response);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

}
