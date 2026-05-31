package com.tissue.feature.wiki.web;

import com.tissue.feature.wiki.application.dto.response.FileDownloadResult;
import com.tissue.feature.wiki.application.dto.response.WikiAttachmentDetailResponse;
import com.tissue.feature.wiki.application.dto.response.WikiAttachmentUploadResponse;
import com.tissue.feature.wiki.application.port.usecase.WikiAttachmentUseCase;
import com.tissue.feature.wiki.domain.exception.WikiErrorCode;
import com.tissue.global.openapi.WikiErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
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
@RequestMapping("/api/v1/wiki/{wikiId}")
@RequiredArgsConstructor
public class WikiAttachmentController {

    private final WikiAttachmentUseCase wikiAttachmentUseCase;

    @Operation(operationId = "uploadWikiAttachment", summary = "Upload wiki file", description = """
                Upload a file to a wiki document.

                **Constraints:**
                - Max file size: 20MB
                - Max attachments per document: 20""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Attachment uploaded"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @WikiErrors({
        WikiErrorCode.DOCUMENT_NOT_FOUND,
        WikiErrorCode.ATTACHMENT_FILE_EMPTY,
        WikiErrorCode.ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED,
        WikiErrorCode.ATTACHMENT_LIMIT_EXCEEDED,
        WikiErrorCode.ATTACHMENT_STORAGE_FAILED,
    })
    @PostMapping("attachments")
    public ResponseEntity<WikiAttachmentUploadResponse> uploadWikiAttachment(
            @PathVariable Long wikiId,
            @RequestParam("file") MultipartFile file,
            @CurrentMember MemberDetails memberDetails) {
        WikiAttachmentUploadResponse response =
                wikiAttachmentUseCase.uploadFile(wikiId, file, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            operationId = "listWikiAttachments",
            summary = "Retrieve wiki file list",
            description = "Retrieve information of all files on a wiki document.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Attachments retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WikiErrors({WikiErrorCode.DOCUMENT_NOT_FOUND})
    @GetMapping("attachments")
    public ResponseEntity<List<WikiAttachmentDetailResponse>> listWikiAttachments(
            @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        List<WikiAttachmentDetailResponse> response =
                wikiAttachmentUseCase.getWikiAttachments(wikiId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "downloadWikiAttachment",
            summary = "Download wiki file",
            description = "Download a file from a wiki document.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File downloaded"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WikiErrors({
        WikiErrorCode.DOCUMENT_NOT_FOUND,
        WikiErrorCode.ATTACHMENT_NOT_FOUND,
    })
    @GetMapping("attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadWikiAttachment(
            @PathVariable Long wikiId, @PathVariable Long attachmentId, @CurrentMember MemberDetails memberDetails) {
        FileDownloadResult result = wikiAttachmentUseCase.download(wikiId, attachmentId, memberDetails.getMemberId());

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(result.originalFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(new InputStreamResource(result.inputStream()));
    }

    @Operation(operationId = "deleteWikiAttachment", summary = "Delete wiki file", description = """
                Permanently delete a file from a wiki document.""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Attachment deleted"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WikiErrors({
        WikiErrorCode.DOCUMENT_NOT_FOUND,
        WikiErrorCode.ATTACHMENT_NOT_FOUND,
    })
    @DeleteMapping("attachments/{attachmentId}")
    public ResponseEntity<Void> deleteWikiAttachment(
            @PathVariable Long wikiId, @PathVariable Long attachmentId, @CurrentMember MemberDetails memberDetails) {
        wikiAttachmentUseCase.deleteAttachment(wikiId, attachmentId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
