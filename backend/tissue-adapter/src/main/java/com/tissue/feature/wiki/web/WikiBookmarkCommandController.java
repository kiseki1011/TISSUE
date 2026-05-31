package com.tissue.feature.wiki.web;

import com.tissue.feature.wiki.application.port.usecase.WikiBookmarkCommandUseCase;
import com.tissue.feature.wiki.domain.exception.WikiErrorCode;
import com.tissue.global.openapi.WikiErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wiki Document")
@RestController
@RequestMapping("/api/v1/wiki")
@RequiredArgsConstructor
public class WikiBookmarkCommandController {

    private final WikiBookmarkCommandUseCase wikiBookmarkCommandUseCase;

    @Operation(
            operationId = "addWikiBookmark",
            summary = "Bookmark document",
            description = "Add a wiki document to the current member's bookmarks.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Document bookmarked"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WikiErrors({WikiErrorCode.DOCUMENT_NOT_FOUND})
    @PutMapping("/{wikiId}/bookmark")
    public ResponseEntity<Void> addWikiBookmark(@PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        wikiBookmarkCommandUseCase.addBookmark(wikiId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "removeWikiBookmark",
            summary = "Remove bookmark",
            description = "Remove a wiki document from the current member's bookmarks.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Bookmark removed"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @DeleteMapping("/{wikiId}/bookmark")
    public ResponseEntity<Void> removeWikiBookmark(
            @PathVariable Long wikiId, @CurrentMember MemberDetails memberDetails) {
        wikiBookmarkCommandUseCase.removeBookmark(wikiId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
