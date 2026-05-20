package com.tissue.feature.issue.web;

import com.tissue.feature.issue.application.dto.response.FileDownloadResult;
import com.tissue.feature.issue.application.dto.response.IssueAttachmentDetailResponse;
import com.tissue.feature.issue.application.dto.response.IssueAttachmentUploadResponse;
import com.tissue.feature.issue.application.port.usecase.IssueAttachmentUseCase;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Issue Attachment")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}")
@RequiredArgsConstructor
public class IssueAttachmentController {

    private final IssueAttachmentUseCase issueAttachmentUseCase;

    @Operation(operationId = "uploadIssueAttachment", summary = "Upload issue file", description = """
                Upload a file to an issue.

                **Constraints:**
                - Max file size: 20MB
                - Max attachments per issue: 20""")
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
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestParam("file") MultipartFile file,
            @CurrentMember MemberDetails memberDetails) {
        IssueAttachmentUploadResponse response = issueAttachmentUseCase.upload(
                IssueIdentifier.of(workspaceKey, issueKey), file, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            operationId = "listIssueAttachments",
            summary = "Retrieve issue file list",
            description = "Retrieve information of all files on an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Attachments retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @GetMapping("attachments")
    public ResponseEntity<List<IssueAttachmentDetailResponse>> listIssueAttachments(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        List<IssueAttachmentDetailResponse> response = issueAttachmentUseCase.getIssueAttachments(
                IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "downloadIssueAttachment",
            summary = "Download issue file",
            description = "Download a file on an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File downloaded"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({IssueErrorCode.ATTACHMENT_NOT_FOUND})
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @GetMapping("attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadIssueAttachment(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long attachmentId,
            @CurrentMember MemberDetails memberDetails) {
        FileDownloadResult result = issueAttachmentUseCase.download(
                IssueIdentifier.of(workspaceKey, issueKey), attachmentId, memberDetails.getMemberId());

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(result.originalFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(new InputStreamResource(result.inputStream()));
    }

    @Operation(operationId = "deleteIssueAttachment", summary = "Delete issue file", description = """
                Permanently delete a file from an issue.

                **Requirements:**
                - Requires workspace `ADMIN`, project `MANAGER`, or the file uploader""")
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
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long attachmentId,
            @CurrentMember MemberDetails memberDetails) {
        issueAttachmentUseCase.delete(
                IssueIdentifier.of(workspaceKey, issueKey), attachmentId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
