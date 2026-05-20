package com.tissue.feature.issuetype.web;

import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.feature.issuetype.application.service.IssueTypeService;
import com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode;
import com.tissue.feature.issuetype.web.request.CreateIssueTypeRequest;
import com.tissue.feature.issuetype.web.request.ReorderFieldsRequest;
import com.tissue.feature.issuetype.web.request.UpdateIssueTypeRequest;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.workflow.domain.exception.WorkflowErrorCode;
import com.tissue.global.openapi.IssueTypeErrors;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.global.openapi.WorkflowErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Custom Issue Type")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class IssueTypeController {

    private final IssueTypeService issueTypeService;

    @Operation(operationId = "createIssueType", summary = "Create issue type", description = """
                Create a new issue type within a project.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Issue type created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @WorkflowErrors({WorkflowErrorCode.WORKFLOW_NOT_FOUND})
    @IssueTypeErrors({IssueTypeErrorCode.DUPLICATE_ISSUE_TYPE_NAME})
    @PostMapping("projects/{projectKey}/issue-types")
    public ResponseEntity<IssueTypeResponse> createIssueType(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid CreateIssueTypeRequest req,
            @CurrentMember MemberDetails memberDetails) {
        var command = req.toCommand();
        IssueTypeResponse response = issueTypeService.create(
                ProjectIdentifier.of(workspaceKey, projectKey), command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateIssueType", summary = "Update issue type", description = """
                Update an issue type's name, description, icon, or color. Only provided fields are updated.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue type updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @IssueTypeErrors({
        IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND,
        IssueTypeErrorCode.DUPLICATE_ISSUE_TYPE_NAME,
    })
    @PatchMapping("issue-types/{issueTypeId}")
    public ResponseEntity<Void> updateIssueType(
            @PathVariable String workspaceKey,
            @PathVariable Long issueTypeId,
            @RequestBody @Valid UpdateIssueTypeRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        issueTypeService.update(workspaceKey, issueTypeId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteIssueType", summary = "Delete issue type", description = """
                Permanently delete an issue type from the project.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue type deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @IssueTypeErrors({
        IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND,
        IssueTypeErrorCode.ISSUE_TYPE_IN_USE,
    })
    @DeleteMapping("issue-types/{issueTypeId}")
    public ResponseEntity<Void> deleteIssueType(
            @PathVariable String workspaceKey,
            @PathVariable Long issueTypeId,
            @CurrentMember MemberDetails memberDetails) {
        issueTypeService.delete(workspaceKey, issueTypeId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "reorderIssueTypeFields", summary = "Reorder fields", description = """
                Reorder the custom fields of an issue type.
                 The request body must contain the ordered list of all field IDs.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Fields reordered"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @IssueTypeErrors({IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND})
    @PostMapping("issue-types/{issueTypeId}:reorderFields")
    public ResponseEntity<Void> reorderIssueTypeFields(
            @PathVariable String workspaceKey,
            @PathVariable Long issueTypeId,
            @RequestBody @Valid ReorderFieldsRequest request,
            @CurrentMember MemberDetails memberDetails) {
        issueTypeService.reorderFields(workspaceKey, issueTypeId, request.orderedIds(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
