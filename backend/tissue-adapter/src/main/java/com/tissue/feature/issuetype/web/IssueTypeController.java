package com.tissue.feature.issuetype.web;

import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.feature.issuetype.application.service.IssueTypeService;
import com.tissue.feature.issuetype.web.request.CreateIssueTypeRequest;
import com.tissue.feature.issuetype.web.request.RenameIssueTypeRequest;
import com.tissue.feature.issuetype.web.request.ReorderFieldsRequest;
import com.tissue.feature.issuetype.web.request.UpdateIssueTypeRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Custom Issue Type")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class IssueTypeController {

    private final IssueTypeService issueTypeService;

    @Operation(summary = "Create issue type", description = """
                Create a new issue type within a project.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Issue type created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "409", description = "Issue type name already exists", content = @Content)
    })
    @PostMapping("projects/{projectKey}/issue-types")
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

    @Operation(summary = "Rename issue type", description = """
                Rename an existing issue type.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue type renamed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue type not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Issue type name already exists", content = @Content)
    })
    @PutMapping("issue-types/{issueTypeId}/rename")
    public ResponseEntity<Void> rename(
            @PathVariable String workspaceKey,
            @PathVariable Long issueTypeId,
            @RequestBody @Valid RenameIssueTypeRequest request,
            @CurrentMember MemberDetails memberDetails) {
        issueTypeService.rename(workspaceKey, issueTypeId, Name.of(request.name()), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update issue type", description = """
                Update an issue type's description, icon, color, or default workflow.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue type updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue type not found", content = @Content)
    })
    @PatchMapping("issue-types/{issueTypeId}")
    public ResponseEntity<Void> update(
            @PathVariable String workspaceKey,
            @PathVariable Long issueTypeId,
            @RequestBody @Valid UpdateIssueTypeRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        issueTypeService.update(workspaceKey, issueTypeId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete issue type", description = """
                Delete an issue type from the project.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue type deleted"),
        @ApiResponse(responseCode = "400", description = "Issue type has active issues", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue type not found", content = @Content)
    })
    @DeleteMapping("issue-types/{issueTypeId}")
    public ResponseEntity<Void> delete(
            @PathVariable String workspaceKey,
            @PathVariable Long issueTypeId,
            @CurrentMember MemberDetails memberDetails) {
        issueTypeService.delete(workspaceKey, issueTypeId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reorder fields", description = """
                Reorder the custom fields of an issue type.
                 The request body must contain the ordered list of all field IDs.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Fields reordered"),
        @ApiResponse(responseCode = "400", description = "Invalid request or missing field IDs", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue type not found", content = @Content)
    })
    @PostMapping("issue-types/{issueTypeId}/issue-fields/reorder")
    public ResponseEntity<Void> reorderFields(
            @PathVariable String workspaceKey,
            @PathVariable Long issueTypeId,
            @RequestBody @Valid ReorderFieldsRequest request,
            @CurrentMember MemberDetails memberDetails) {
        issueTypeService.reorderFields(workspaceKey, issueTypeId, request.orderedIds(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
