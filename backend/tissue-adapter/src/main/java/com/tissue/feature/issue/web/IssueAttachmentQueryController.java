package com.tissue.feature.issue.web;

import com.tissue.feature.issue.application.dto.response.FileDownloadResult;
import com.tissue.feature.issue.application.dto.response.IssueAttachmentDetailResponse;
import com.tissue.feature.issue.application.port.usecase.IssueAttachmentQueryUseCase;
import com.tissue.feature.issue.domain.exception.IssueErrorCode;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.global.openapi.IssueErrors;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Issue Attachment")
@RestController
@RequestMapping("/api/v1/issues/{issueKey}")
@RequiredArgsConstructor
public class IssueAttachmentQueryController {

    private final IssueAttachmentQueryUseCase issueAttachmentQueryUseCase;

    @Operation(operationId = "listIssueAttachments", summary = "List issue attachments", description = """
                    List all attachments of an issue.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Attachments retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @GetMapping("attachments")
    public ResponseEntity<List<IssueAttachmentDetailResponse>> listIssueAttachments(
            @PathVariable String issueKey, @CurrentMember MemberDetails memberDetails) {
        List<IssueAttachmentDetailResponse> response = issueAttachmentQueryUseCase.getIssueAttachments(
                IssueIdentifier.ofIssueKey(issueKey), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "downloadIssueAttachment", summary = "Download issue attachment", description = """
                    Download a file attached to an issue.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File downloaded"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueErrors({IssueErrorCode.ATTACHMENT_NOT_FOUND})
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @GetMapping("attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadIssueAttachment(
            @PathVariable String issueKey,
            @PathVariable Long attachmentId,
            @CurrentMember MemberDetails memberDetails) {
        FileDownloadResult result = issueAttachmentQueryUseCase.download(
                IssueIdentifier.ofIssueKey(issueKey), attachmentId, memberDetails.getMemberId());

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(result.originalFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(new InputStreamResource(result.inputStream()));
    }
}
