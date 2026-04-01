package com.tissue.feature.attachment.web;

import com.tissue.feature.attachment.application.dto.request.UploadAttachmentCommand;
import com.tissue.feature.attachment.application.dto.response.AttachmentDetailResponse;
import com.tissue.feature.attachment.application.dto.response.AttachmentUploadResponse;
import com.tissue.feature.attachment.application.dto.response.FileDownloadResult;
import com.tissue.feature.attachment.application.port.usecase.AttachmentCommandUseCase;
import com.tissue.feature.attachment.application.port.usecase.AttachmentQueryUseCase;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
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

    private final AttachmentCommandUseCase attachmentCommandUseCase;
    private final AttachmentQueryUseCase attachmentQueryUseCase;

    @Operation(summary = "Upload issue file", description = """
                Upload a file to an issue.

                **Constraints:**
                - Max file size: 20MB
                - Max attachments per issue: 20
                - Allowed content types: `image/png`, `image/jpeg`, `image/gif`, `image/webp`, \
                `application/pdf`, `text/plain`, `text/csv`, `application/json`, `application/xml`, \
                `application/zip`, `application/gzip`""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Attachment uploaded"),
        @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @PostMapping("attachments")
    public ResponseEntity<AttachmentUploadResponse> uploadAttachment(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestParam("file") MultipartFile file,
            @CurrentMember MemberDetails memberDetails)
            throws IOException {
        UploadAttachmentCommand command = new UploadAttachmentCommand(
                file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getInputStream());

        AttachmentUploadResponse response = attachmentCommandUseCase.upload(
                IssueIdentifier.of(workspaceKey, issueKey), command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Retrieve issue file list", description = "Retrieve information of all files on an issue.")
    @ApiResponse(responseCode = "200", description = "Attachments retrieved")
    @GetMapping("attachments")
    public ResponseEntity<List<AttachmentDetailResponse>> getAttachments(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        List<AttachmentDetailResponse> response = attachmentQueryUseCase.getIssueAttachments(
                IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Download issue file", description = "Download a file on an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File downloaded"),
        @ApiResponse(responseCode = "404", description = "Attachment not found", content = @Content)
    })
    @GetMapping("attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long attachmentId,
            @CurrentMember MemberDetails memberDetails) {
        FileDownloadResult result = attachmentQueryUseCase.download(
                IssueIdentifier.of(workspaceKey, issueKey), attachmentId, memberDetails.getMemberId());

        String encodedFilename = URLEncoder.encode(result.originalFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(new InputStreamResource(result.inputStream()));
    }

    @Operation(summary = "Delete issue file", description = "Delete a file from an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Attachment deleted"),
        @ApiResponse(responseCode = "404", description = "Attachment not found", content = @Content)
    })
    @DeleteMapping("attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long attachmentId,
            @CurrentMember MemberDetails memberDetails) {
        attachmentCommandUseCase.delete(
                IssueIdentifier.of(workspaceKey, issueKey), attachmentId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
