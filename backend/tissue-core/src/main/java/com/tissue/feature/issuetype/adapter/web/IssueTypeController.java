package com.tissue.feature.issuetype.adapter.web;

import com.tissue.feature.issuetype.adapter.web.request.CreateIssueTypeRequest;
import com.tissue.feature.issuetype.adapter.web.request.ReorderFieldsRequest;
import com.tissue.feature.issuetype.adapter.web.request.UpdateIssueTypeRequest;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.feature.issuetype.application.service.IssueTypeService;
import com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.feature.workflow.domain.exception.WorkflowErrorCode;
import com.tissue.global.openapi.IssueTypeErrors;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.global.openapi.WorkflowErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.auth.RequireSystemAdmin;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IssueTypeController {

    private final IssueTypeService issueTypeService;

    @Operation(operationId = "createIssueType", summary = "Create issue type", description = """
                Create a new global issue type.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Issue type created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @WorkflowErrors({WorkflowErrorCode.WORKFLOW_NOT_FOUND})
    @IssueTypeErrors({IssueTypeErrorCode.DUPLICATE_ISSUE_TYPE_NAME})
    @RequireSystemAdmin
    @PostMapping("/issue-types")
    public ResponseEntity<IssueTypeResponse> createIssueType(
            @RequestBody @Valid CreateIssueTypeRequest req, @CurrentMember MemberDetails memberDetails) {
        var command = req.toCommand();
        IssueTypeResponse response = issueTypeService.create(command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateIssueType", summary = "Update issue type", description = """
                Update an issue type's name, description, icon, or color. Only provided fields are updated.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue type updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @IssueTypeErrors({
        IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND,
        IssueTypeErrorCode.DUPLICATE_ISSUE_TYPE_NAME,
    })
    @RequireSystemAdmin
    @PatchMapping("/issue-types/{issueTypeId}")
    public ResponseEntity<Void> updateIssueType(
            @PathVariable Long issueTypeId,
            @RequestBody @Valid UpdateIssueTypeRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        issueTypeService.update(issueTypeId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteIssueType", summary = "Delete issue type", description = """
                Permanently delete a global issue type.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue type deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @IssueTypeErrors({
        IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND,
        IssueTypeErrorCode.ISSUE_TYPE_IN_USE,
    })
    @RequireSystemAdmin
    @DeleteMapping("/issue-types/{issueTypeId}")
    public ResponseEntity<Void> deleteIssueType(
            @PathVariable Long issueTypeId, @CurrentMember MemberDetails memberDetails) {
        issueTypeService.delete(issueTypeId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "reorderIssueTypeFields", summary = "Reorder fields", description = """
                Reorder the custom fields of an issue type.
                 The request body must contain the ordered list of all field IDs.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Fields reordered"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @IssueTypeErrors({IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND})
    @RequireSystemAdmin
    @PostMapping("/issue-types/{issueTypeId}:reorderFields")
    public ResponseEntity<Void> reorderIssueTypeFields(
            @PathVariable Long issueTypeId,
            @RequestBody @Valid ReorderFieldsRequest request,
            @CurrentMember MemberDetails memberDetails) {
        issueTypeService.reorderFields(issueTypeId, request.orderedIds(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
