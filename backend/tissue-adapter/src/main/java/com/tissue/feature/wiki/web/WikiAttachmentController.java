package com.tissue.feature.wiki.web;

import com.tissue.feature.wiki.application.dto.response.FileDownloadResult;
import com.tissue.feature.wiki.application.dto.response.WikiAttachmentDetailResponse;
import com.tissue.feature.wiki.application.dto.response.WikiAttachmentUploadResponse;
import com.tissue.feature.wiki.application.port.usecase.WikiAttachmentUseCase;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
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

@Tag(name = "Wiki Attachment")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/wiki/{wikiId}")
@RequiredArgsConstructor
public class WikiAttachmentController {

    private final WikiAttachmentUseCase wikiAttachmentUseCase;

    @Operation(summary = "Upload wiki file", description = """
                Upload a file to a wiki document.

                **Constraints:**
                - Max file size: 20MB
                - Max attachments per document: 20""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Attachment uploaded"),
        @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
    })
    @PostMapping("attachments")
    public ResponseEntity<WikiAttachmentUploadResponse> uploadAttachment(
            @PathVariable String workspaceKey,
            @PathVariable Long wikiId,
            @RequestParam("file") MultipartFile file,
            @CurrentMember MemberDetails memberDetails) {
        WikiAttachmentUploadResponse response =
                wikiAttachmentUseCase.uploadFile(workspaceKey, wikiId, file, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Retrieve wiki file list",
            description = "Retrieve information of all files on a wiki document.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Attachments retrieved"),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
    })
    @GetMapping("attachments")
    public ResponseEntity<List<WikiAttachmentDetailResponse>> getAttachments(
            @PathVariable String workspaceKey, @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        List<WikiAttachmentDetailResponse> response =
                wikiAttachmentUseCase.getWikiAttachments(workspaceKey, wikiId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Download wiki file", description = "Download a file from a wiki document.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File downloaded"),
        @ApiResponse(responseCode = "404", description = "Attachment not found", content = @Content)
    })
    @GetMapping("attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable String workspaceKey,
            @PathVariable Long wikiId,
            @PathVariable Long attachmentId,
            @CurrentMember MemberDetails memberDetails) {
        FileDownloadResult result =
                wikiAttachmentUseCase.download(workspaceKey, wikiId, attachmentId, memberDetails.getMemberId());

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(result.originalFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(new InputStreamResource(result.inputStream()));
    }

    @Operation(summary = "Delete wiki file", description = """
                Permanently delete a file from a wiki document.""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Attachment deleted"),
        @ApiResponse(responseCode = "404", description = "Attachment not found", content = @Content)
    })
    @DeleteMapping("attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable String workspaceKey,
            @PathVariable Long wikiId,
            @PathVariable Long attachmentId,
            @CurrentMember MemberDetails memberDetails) {
        wikiAttachmentUseCase.deleteAttachment(workspaceKey, wikiId, attachmentId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
