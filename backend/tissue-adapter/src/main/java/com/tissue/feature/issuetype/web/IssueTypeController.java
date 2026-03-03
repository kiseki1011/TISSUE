package com.tissue.feature.issuetype.web;

import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.feature.issuetype.application.service.IssueTypeService;
import com.tissue.feature.issuetype.web.request.CreateIssueTypeRequest;
import com.tissue.feature.issuetype.web.request.RenameIssueTypeRequest;
import com.tissue.feature.issuetype.web.request.ReorderFieldsRequest;
import com.tissue.feature.issuetype.web.request.UpdateIssueTypeRequest;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/issue-types")
@RequiredArgsConstructor
public class IssueTypeController {

    private final IssueTypeService issueTypeService;

    @PostMapping
    public ResponseEntity<IssueTypeResponse> create(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid CreateIssueTypeRequest req,
            @CurrentMember MemberDetails memberDetails) {

        var command = req.toCommand();
        IssueTypeResponse response = issueTypeService.create(
                ProjectIdentifier.of(workspaceKey, projectKey), command, memberDetails.getMemberId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.issueTypeId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{issueTypeId}/rename")
    public ResponseEntity<Void> rename(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @RequestBody @Valid RenameIssueTypeRequest request,
            @CurrentMember MemberDetails memberDetails) {

        issueTypeService.rename(
                ProjectIdentifier.of(workspaceKey, projectKey),
                issueTypeId,
                Name.of(request.name()),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{issueTypeId}")
    public ResponseEntity<Void> update(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @RequestBody @Valid UpdateIssueTypeRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        issueTypeService.update(
                ProjectIdentifier.of(workspaceKey, projectKey), issueTypeId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueTypeId}")
    public ResponseEntity<Void> delete(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @CurrentMember MemberDetails memberDetails) {

        issueTypeService.delete(
                ProjectIdentifier.of(workspaceKey, projectKey), issueTypeId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueTypeId}/issue-fields/reorder")
    public ResponseEntity<Void> reorderFields(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @RequestBody @Valid ReorderFieldsRequest request,
            @CurrentMember MemberDetails memberDetails) {

        issueTypeService.reorderFields(
                ProjectIdentifier.of(workspaceKey, projectKey),
                issueTypeId,
                request.orderedIds(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
