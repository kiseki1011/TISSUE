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

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/issues/{issueKey}")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentCommandUseCase attachmentCommandUseCase;
    private final AttachmentQueryUseCase attachmentQueryUseCase;

    @PostMapping("/attachments")
    public ResponseEntity<AttachmentUploadResponse> uploadAttachment(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestParam("file") MultipartFile file,
            @CurrentMember MemberDetails memberDetails)
            throws IOException {
        UploadAttachmentCommand command = new UploadAttachmentCommand(
                file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getInputStream());

        AttachmentUploadResponse response = attachmentCommandUseCase.upload(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/attachments")
    public ResponseEntity<List<AttachmentDetailResponse>> getAttachments(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        List<AttachmentDetailResponse> response = attachmentQueryUseCase.getIssueAttachments(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @PathVariable Long attachmentId,
            @CurrentMember MemberDetails memberDetails) {
        FileDownloadResult result = attachmentQueryUseCase.download(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), attachmentId, memberDetails.getMemberId());

        String encodedFilename = URLEncoder.encode(result.originalFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(new InputStreamResource(result.inputStream()));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @PathVariable Long attachmentId,
            @CurrentMember MemberDetails memberDetails) {
        attachmentCommandUseCase.delete(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), attachmentId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
