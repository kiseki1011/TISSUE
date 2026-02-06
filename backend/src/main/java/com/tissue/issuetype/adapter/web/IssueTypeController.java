package com.tissue.issuetype.adapter.web;

import com.tissue.global.vo.Name;
import com.tissue.issuetype.adapter.web.request.CreateIssueTypeRequest;
import com.tissue.issuetype.adapter.web.request.RenameIssueTypeRequest;
import com.tissue.issuetype.adapter.web.request.UpdateIssueTypeRequest;
import com.tissue.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.issuetype.application.service.IssueTypeService;
import com.tissue.workspace.adapter.web.resolver.CurrentWorkspaceMember;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
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
            @PathVariable String projectKey,
            @RequestBody @Valid CreateIssueTypeRequest req,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        var command = req.toCommand();
        IssueTypeResponse response = issueTypeService.create(projectKey, command, currentWorkspaceMember);

        // TODO: created 사용

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{issueTypeId}/rename")
    public ResponseEntity<Void> rename(
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @RequestBody @Valid RenameIssueTypeRequest request,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        issueTypeService.rename(projectKey, issueTypeId, Name.of(request.name()), currentWorkspaceMember);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{issueTypeId}")
    public ResponseEntity<Void> update(
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @RequestBody @Valid UpdateIssueTypeRequest request,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        var command = request.toCommand();
        issueTypeService.update(projectKey, issueTypeId, command, currentWorkspaceMember);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueTypeId}")
    public ResponseEntity<Void> delete(
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        issueTypeService.delete(projectKey, issueTypeId, currentWorkspaceMember);
        return ResponseEntity.noContent().build();
    }
}
