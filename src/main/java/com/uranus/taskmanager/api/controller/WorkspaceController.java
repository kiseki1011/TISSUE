package com.uranus.taskmanager.api.controller;

import com.uranus.taskmanager.api.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.response.WorkspaceResponse;
import com.uranus.taskmanager.api.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
