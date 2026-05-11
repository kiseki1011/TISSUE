package com.tissue.feature.wiki.web;

import com.tissue.feature.wiki.application.dto.response.WikiBookmarkResponse;
import com.tissue.feature.wiki.application.port.usecase.WikiBookmarkUseCase;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wiki Document")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/wiki")
@RequiredArgsConstructor
public class WikiBookmarkController {

    private final WikiBookmarkUseCase wikiBookmarkUseCase;

    @Operation(
            operationId = "addWikiBookmark",
            summary = "Bookmark document",
            description = "Add a wiki document to the current member's bookmarks.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Document bookmarked"),
        @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
    })
    @PutMapping("/{wikiId}/bookmark")
    public ResponseEntity<Void> addWikiBookmark(
            @PathVariable String workspaceKey, @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        wikiBookmarkUseCase.addBookmark(workspaceKey, wikiId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "removeWikiBookmark",
            summary = "Remove bookmark",
            description = "Remove a wiki document from the current member's bookmarks.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Bookmark removed")})
    @DeleteMapping("/{wikiId}/bookmark")
    public ResponseEntity<Void> removeWikiBookmark(
            @PathVariable String workspaceKey, @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        wikiBookmarkUseCase.removeBookmark(workspaceKey, wikiId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "listWikiBookmarks",
            summary = "Get bookmarked documents",
            description = "Retrieve all wiki documents bookmarked by the current member.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Bookmarked documents retrieved")})
    @GetMapping("/bookmarks")
    public ResponseEntity<List<WikiBookmarkResponse>> listWikiBookmarks(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        List<WikiBookmarkResponse> response =
                wikiBookmarkUseCase.getBookmarks(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
