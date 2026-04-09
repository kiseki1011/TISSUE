package com.tissue.feature.wiki.web;

import com.tissue.feature.wiki.application.dto.response.DocumentResponse;
import com.tissue.feature.wiki.application.port.usecase.WikiCommandUseCase;
import com.tissue.feature.wiki.web.request.AddWikiLinkRequest;
import com.tissue.feature.wiki.web.request.CreateDocumentRequest;
import com.tissue.feature.wiki.web.request.SetDocumentParentRequest;
import com.tissue.feature.wiki.web.request.UpdateDocumentContentRequest;
import com.tissue.feature.wiki.web.request.UpdateDocumentTitleRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Wiki Document")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/wiki")
@RequiredArgsConstructor
public class WikiDocumentCommandController {

    private final WikiCommandUseCase wikiCommandUseCase;

    @Operation(
            summary = "Create document",
            description = "Create a new wiki document. Can optionally specify a parent document.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Document created"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request or parent workspace mismatch",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Parent document not found", content = @Content)
    })
    @PostMapping
    public ResponseEntity<DocumentResponse> create(
            @PathVariable String workspaceKey,
            @RequestBody @Valid CreateDocumentRequest request,
            @CurrentMember MemberDetails memberDetails) {
        DocumentResponse response =
                wikiCommandUseCase.create(workspaceKey, request.toCommand(), memberDetails.getMemberId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{wikiId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Update document title", description = "Update the title of a wiki document.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Title updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request or document is locked", content = @Content),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
    })
    @PatchMapping("/{wikiId}/title")
    public ResponseEntity<Void> updateTitle(
            @PathVariable String workspaceKey,
            @PathVariable Long wikiId,
            @RequestBody @Valid UpdateDocumentTitleRequest request,
            @CurrentMember MemberDetails memberDetails) {
        wikiCommandUseCase.updateTitle(workspaceKey, wikiId, request.title(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update document content",
            description = "Update the content of a wiki document. A version snapshot is created automatically.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Content updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request or document is locked", content = @Content),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
    })
    @PatchMapping("/{wikiId}/content")
    public ResponseEntity<Void> updateContent(
            @PathVariable String workspaceKey,
            @PathVariable Long wikiId,
            @RequestBody @Valid UpdateDocumentContentRequest request,
            @CurrentMember MemberDetails memberDetails) {
        wikiCommandUseCase.updateContent(workspaceKey, wikiId, request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Set parent document",
            description = "Set or detach the parent document. Use null for `parentDocumentId` to detach from parent.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Parent updated"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request or parent workspace mismatch",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Document or parent not found", content = @Content)
    })
    @PutMapping("/{wikiId}/parent")
    public ResponseEntity<Void> setParent(
            @PathVariable String workspaceKey,
            @PathVariable Long wikiId,
            @RequestBody @Valid SetDocumentParentRequest request,
            @CurrentMember MemberDetails memberDetails) {
        wikiCommandUseCase.setParent(workspaceKey, wikiId, request.parentDocumentId(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add link", description = """
                Add a link to another resource (issue, project, or wiki document).

                **Requirements:**
                - Cannot link a wiki document to itself
                - Target resource must belong to the same workspace""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Link added"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request, self-reference, or resource workspace mismatch",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Document or target not found", content = @Content)
    })
    @PostMapping("/{wikiId}/links")
    public ResponseEntity<Void> addLink(
            @PathVariable String workspaceKey,
            @PathVariable Long wikiId,
            @RequestBody @Valid AddWikiLinkRequest request,
            @CurrentMember MemberDetails memberDetails) {
        wikiCommandUseCase.addLink(
                workspaceKey, wikiId, request.targetType(), request.targetId(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove link", description = "Remove a link from this document.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Link removed"),
        @ApiResponse(responseCode = "404", description = "Document or link not found", content = @Content)
    })
    @DeleteMapping("/{wikiId}/links/{linkId}")
    public ResponseEntity<Void> removeLink(
            @PathVariable String workspaceKey,
            @PathVariable Long wikiId,
            @PathVariable Long linkId,
            @CurrentMember MemberDetails memberDetails) {
        wikiCommandUseCase.removeLink(workspaceKey, wikiId, linkId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lock document", description = """
                Lock a document to prevent edits.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role, or document creator""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Document locked"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
    })
    @PostMapping("/{wikiId}:lock")
    public ResponseEntity<Void> lock(
            @PathVariable String workspaceKey, @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        wikiCommandUseCase.lock(workspaceKey, wikiId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unlock document", description = """
                Unlock a document to allow edits.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role, or document creator""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Document unlocked"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
    })
    @PostMapping("/{wikiId}:unlock")
    public ResponseEntity<Void> unlock(
            @PathVariable String workspaceKey, @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        wikiCommandUseCase.unLock(workspaceKey, wikiId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Soft delete document", description = """
                Soft-delete a document. Can be restored later.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role, or document creator""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Document deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
    })
    @DeleteMapping("/{wikiId}")
    public ResponseEntity<Void> delete(
            @PathVariable String workspaceKey, @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        wikiCommandUseCase.delete(workspaceKey, wikiId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restore document", description = """
                Restore a soft-deleted document.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role, or document creator""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Document restored"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
    })
    @PostMapping("/{wikiId}:restore")
    public ResponseEntity<Void> restore(
            @PathVariable String workspaceKey, @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        wikiCommandUseCase.restore(workspaceKey, wikiId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Permanently delete document", description = """
                Permanently delete a soft-deleted document.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role, or document creator
                - Document must be soft-deleted
                - Document must not have children""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Document permanently deleted"),
        @ApiResponse(responseCode = "400", description = "Document has children", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Document not found in trash", content = @Content)
    })
    @DeleteMapping("/trash/{wikiId}")
    public ResponseEntity<Void> hardDelete(
            @PathVariable String workspaceKey, @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        wikiCommandUseCase.hardDelete(workspaceKey, wikiId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Permanently delete all soft-deleted documents ", description = """
                Permanently delete all soft-deleted documents in the workspace.

                **Requirements:**
                - Checks delete permission per document (`ADMIN` or higher role, or document creator)""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Trash emptied"),
        @ApiResponse(
                responseCode = "403",
                description = "Insufficient permission for one or more documents",
                content = @Content)
    })
    @DeleteMapping("/trash")
    public ResponseEntity<Void> batchHardDelete(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        wikiCommandUseCase.batchHardDelete(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
