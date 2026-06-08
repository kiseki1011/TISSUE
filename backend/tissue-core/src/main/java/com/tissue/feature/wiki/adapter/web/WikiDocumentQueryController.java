package com.tissue.feature.wiki.adapter.web;

import com.tissue.feature.wiki.application.dto.response.WikiDocumentDetail;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentSearchResult;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentSummary;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentTreeNode;
import com.tissue.feature.wiki.application.dto.response.WikiSnapshotDetail;
import com.tissue.feature.wiki.application.dto.response.WikiSnapshotSummary;
import com.tissue.feature.wiki.application.port.usecase.WikiQueryUseCase;
import com.tissue.feature.wiki.domain.exception.WikiErrorCode;
import com.tissue.global.openapi.WikiErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.KeysetPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wiki Document")
@Validated
@RestController
@RequestMapping("/api/v1/wiki")
@RequiredArgsConstructor
public class WikiDocumentQueryController {

    private final WikiQueryUseCase wikiQueryUseCase;

    @Operation(
            operationId = "getWikiDocument",
            summary = "Get document detail",
            description = "Retrieve a wiki document with its links and parent info.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WikiErrors({WikiErrorCode.DOCUMENT_NOT_FOUND})
    @GetMapping("/{wikiId}")
    public ResponseEntity<WikiDocumentDetail> getWikiDocument(
            @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        WikiDocumentDetail response = wikiQueryUseCase.getDocumentDetail(wikiId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "listRootWikiDocuments",
            summary = "Get root documents",
            description = "Retrieve root documents that have no parent.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Root documents retrieved")})
    @GetMapping("/roots")
    public ResponseEntity<List<WikiDocumentSummary>> listRootWikiDocuments(@CurrentMember MemberDetails memberDetails) {
        List<WikiDocumentSummary> response = wikiQueryUseCase.getRootDocuments(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "listWikiDocumentChildren",
            summary = "Get child documents",
            description = "Retrieve child documents of a given parent document.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Child documents retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WikiErrors({WikiErrorCode.DOCUMENT_NOT_FOUND})
    @GetMapping("/{wikiId}/children")
    public ResponseEntity<List<WikiDocumentSummary>> listWikiDocumentChildren(
            @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        List<WikiDocumentSummary> response = wikiQueryUseCase.getChildrenDocuments(wikiId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "getWikiDocumentTree",
            summary = "Get document tree",
            description = "Retrieve a list of all documents with parent references.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Document tree retrieved")})
    @GetMapping("/tree")
    public ResponseEntity<List<WikiDocumentTreeNode>> getWikiDocumentTree(@CurrentMember MemberDetails memberDetails) {
        List<WikiDocumentTreeNode> response = wikiQueryUseCase.getDocumentTree(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "listWikiDocumentVersions",
            summary = "Get version history",
            description = "Retrieve the version history (snapshots) of a document.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Version history retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WikiErrors({WikiErrorCode.DOCUMENT_NOT_FOUND})
    @GetMapping("/{wikiId}/versions")
    public ResponseEntity<List<WikiSnapshotSummary>> listWikiDocumentVersions(
            @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        List<WikiSnapshotSummary> response = wikiQueryUseCase.getVersionHistory(wikiId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "getWikiDocumentVersion",
            summary = "Get version snapshot",
            description = "Retrieve a specific version snapshot of document including its content.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Snapshot retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WikiErrors({
        WikiErrorCode.DOCUMENT_NOT_FOUND,
        WikiErrorCode.SNAPSHOT_NOT_FOUND,
    })
    @GetMapping("/{wikiId}/versions/{snapshotId}")
    public ResponseEntity<WikiSnapshotDetail> getWikiDocumentVersion(
            @PathVariable Long wikiId, @PathVariable Long snapshotId, @CurrentMember MemberDetails memberDetails) {
        WikiSnapshotDetail response =
                wikiQueryUseCase.getVersionSnapshotDetail(wikiId, snapshotId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "searchWikiDocuments",
            summary = "Search documents",
            description = "Search documents by keyword (title/content).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @GetMapping("/search")
    public ResponseEntity<KeysetPageResponse<WikiDocumentSearchResult>> searchWikiDocuments(
            @Parameter(description = "Search keyword (title/content). Optional when filtering by tags.")
                    @RequestParam(required = false)
                    @Size(max = 200)
                    @Nullable
                    String keyword,
            @Parameter(description = "Filter by tag IDs (matches documents having any of them)")
                    @RequestParam(required = false)
                    @Nullable
                    Set<Long> tagIds,
            @Parameter(description = "Last modified timestamp from the previous page") @RequestParam(required = false)
                    Instant keysetModifiedAt,
            @Parameter(description = "Last wiki document ID from the previous page") @RequestParam(required = false)
                    Long keysetDocumentId,
            @Parameter(description = "Number of documents per page", example = "20")
                    @RequestParam(defaultValue = "20")
                    @Max(100)
                    int limit,
            @CurrentMember MemberDetails memberDetails) {
        KeysetPageResponse<WikiDocumentSearchResult> response = wikiQueryUseCase.searchDocuments(
                keyword, tagIds, memberDetails.getMemberId(), keysetModifiedAt, keysetDocumentId, limit);

        return ResponseEntity.ok(response);
    }
}
