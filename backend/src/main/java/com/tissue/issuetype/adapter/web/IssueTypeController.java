package com.tissue.issuetype.adapter.web;

import com.tissue.issuetype.adapter.web.request.CreateIssueTypeRequest;
import com.tissue.issuetype.adapter.web.request.RenameIssueTypeRequest;
import com.tissue.issuetype.adapter.web.request.UpdateIssueTypeRequest;
import com.tissue.issuetype.application.dto.request.DeleteIssueTypeCommand;
import com.tissue.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.issuetype.application.service.IssueTypeService;
import com.tissue.project.adapter.web.resolver.CurrentProjectMember;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/issuetypes")
@RequiredArgsConstructor
public class IssueTypeController {

    private final IssueTypeService issueTypeService;

    @PostMapping
    public ResponseEntity<IssueTypeResponse> create(
        @PathVariable String workspaceKey,
        @PathVariable String projectKey,
        @RequestBody @Valid CreateIssueTypeRequest req,
        @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = req.toCommand(workspaceKey, projectKey, actorContext);
        IssueTypeResponse response = issueTypeService.create(command);

        // TODO: created 사용

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/rename")
    public ResponseEntity<Void> rename(
        @PathVariable String workspaceKey,
        @PathVariable String projectKey,
        @PathVariable Long id,
        @RequestBody @Valid RenameIssueTypeRequest request,
        @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand(workspaceKey, projectKey, id, actorContext);
        issueTypeService.rename(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(
        @PathVariable String workspaceKey,
        @PathVariable String projectKey,
        @PathVariable Long id,
        @RequestBody @Valid UpdateIssueTypeRequest request,
        @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand(workspaceKey, projectKey, id, actorContext);
        issueTypeService.update(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable String workspaceKey,
        @PathVariable String projectKey,
        @PathVariable Long id,
        @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = new DeleteIssueTypeCommand(workspaceKey, projectKey, id, actorContext);
        issueTypeService.delete(command);

        return ResponseEntity.noContent().build();
    }
}
