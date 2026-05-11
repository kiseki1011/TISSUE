package com.tissue.feature.wiki.web;

import com.tissue.feature.wiki.application.dto.response.WikiDocumentDetail;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentSearchResult;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentSummary;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentTreeNode;
import com.tissue.feature.wiki.application.dto.response.WikiSnapshotDetail;
import com.tissue.feature.wiki.application.dto.response.WikiSnapshotSummary;
import com.tissue.feature.wiki.application.port.usecase.WikiQueryUseCase;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
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
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/workspaces/{workspaceKey}/wiki")
@RequiredArgsConstructor
public class WikiDocumentQueryController {

    private final WikiQueryUseCase wikiQueryUseCase;

    @Operation(
            operationId = "getWikiDocument",
            summary = "Get document detail",
            description = "Retrieve a wiki document with its links and parent info.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document retrieved"),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
    })
    @GetMapping("/{wikiId}")
    public ResponseEntity<WikiDocumentDetail> getWikiDocument(
            @PathVariable String workspaceKey, @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        WikiDocumentDetail response =
                wikiQueryUseCase.getDocumentDetail(workspaceKey, wikiId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "listRootWikiDocuments",
            summary = "Get root documents",
            description = "Retrieve root documents that have no parent.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Root documents retrieved")})
    @GetMapping("/roots")
    public ResponseEntity<List<WikiDocumentSummary>> listRootWikiDocuments(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        List<WikiDocumentSummary> response =
                wikiQueryUseCase.getRootDocuments(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "listWikiDocumentChildren",
            summary = "Get child documents",
            description = "Retrieve child documents of a given parent document.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Child documents retrieved"),
        @ApiResponse(responseCode = "404", description = "Parent document not found", content = @Content)
    })
    @GetMapping("/{wikiId}/children")
    public ResponseEntity<List<WikiDocumentSummary>> listWikiDocumentChildren(
            @PathVariable String workspaceKey, @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        List<WikiDocumentSummary> response =
                wikiQueryUseCase.getChildrenDocuments(workspaceKey, wikiId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "getWikiDocumentTree",
            summary = "Get document tree",
            description = "Retrieve a list of all documents with parent references.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Document tree retrieved")})
    @GetMapping("/tree")
    public ResponseEntity<List<WikiDocumentTreeNode>> getWikiDocumentTree(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        List<WikiDocumentTreeNode> response =
                wikiQueryUseCase.getDocumentTree(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "listWikiDocumentVersions",
            summary = "Get version history",
            description = "Retrieve the version history (snapshots) of a document.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Version history retrieved"),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
    })
    @GetMapping("/{wikiId}/versions")
    public ResponseEntity<List<WikiSnapshotSummary>> listWikiDocumentVersions(
            @PathVariable String workspaceKey, @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        List<WikiSnapshotSummary> response =
                wikiQueryUseCase.getVersionHistory(workspaceKey, wikiId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "getWikiDocumentVersion",
            summary = "Get version snapshot",
            description = "Retrieve a specific version snapshot of document including its content.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Snapshot retrieved"),
        @ApiResponse(responseCode = "404", description = "Document or snapshot not found", content = @Content)
    })
    @GetMapping("/{wikiId}/versions/{snapshotId}")
    public ResponseEntity<WikiSnapshotDetail> getWikiDocumentVersion(
            @PathVariable String workspaceKey,
            @PathVariable Long wikiId,
            @PathVariable Long snapshotId,
            @CurrentMember MemberDetails memberDetails) {
        WikiSnapshotDetail response = wikiQueryUseCase.getVersionSnapshotDetail(
                workspaceKey, wikiId, snapshotId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "searchWikiDocuments",
            summary = "Search documents",
            description = "Search documents by keyword in title or content.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Search results retrieved")})
    @GetMapping("/search")
    public ResponseEntity<KeysetPageResponse<WikiDocumentSearchResult>> searchWikiDocuments(
            @PathVariable String workspaceKey,
            @Parameter(description = "Search keyword") @RequestParam @Size(min = 1, max = 200) String keyword,
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
                workspaceKey, keyword, memberDetails.getMemberId(), keysetModifiedAt, keysetDocumentId, limit);

        return ResponseEntity.ok(response);
    }
}
