package com.tissue.feature.issue.web;

import com.tissue.feature.issue.application.dto.response.IssueAttachmentUploadResponse;
import com.tissue.feature.issue.application.port.usecase.IssueAttachmentCommandUseCase;
import com.tissue.feature.issue.domain.exception.IssueErrorCode;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.global.openapi.IssueErrors;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Issue Attachment")
@RestController
@RequestMapping("/api/v1/issues/{issueKey}")
@RequiredArgsConstructor
public class IssueAttachmentCommandController {

    private final IssueAttachmentCommandUseCase issueAttachmentCommandUseCase;

    @Operation(operationId = "uploadIssueAttachment", summary = "Upload issue file", description = """
                Upload a file to an issue.

                **Constraints:**
                - Max file size: 20MB
                - Max attachments per issue: 20

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Attachment uploaded"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ISSUE_NOT_FOUND,
        IssueErrorCode.ATTACHMENT_FILE_EMPTY,
        IssueErrorCode.ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED,
        IssueErrorCode.ATTACHMENT_LIMIT_EXCEEDED,
        IssueErrorCode.ATTACHMENT_STORAGE_FAILED,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PostMapping("attachments")
    public ResponseEntity<IssueAttachmentUploadResponse> uploadIssueAttachment(
            @PathVariable String issueKey,
            @RequestParam("file") MultipartFile file,
            @CurrentMember MemberDetails memberDetails) {
        IssueAttachmentUploadResponse response = issueAttachmentCommandUseCase.upload(
                IssueIdentifier.ofIssueKey(issueKey), file, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "deleteIssueAttachment", summary = "Delete issue file", description = """
                Permanently delete a file from an issue.

                **Requirements:**
                - Requires system `ADMIN`, project `MANAGER`, or the file uploader""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Attachment deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({
        IssueErrorCode.ATTACHMENT_NOT_FOUND,
        IssueErrorCode.ATTACHMENT_DELETE_NOT_ALLOWED,
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @DeleteMapping("attachments/{attachmentId}")
    public ResponseEntity<Void> deleteIssueAttachment(
            @PathVariable String issueKey,
            @PathVariable Long attachmentId,
            @CurrentMember MemberDetails memberDetails) {
        issueAttachmentCommandUseCase.delete(
                IssueIdentifier.ofIssueKey(issueKey), attachmentId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
