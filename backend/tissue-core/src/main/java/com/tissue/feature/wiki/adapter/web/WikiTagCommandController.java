package com.tissue.feature.wiki.adapter.web;

import com.tissue.feature.wiki.adapter.web.request.AttachWikiTagRequest;
import com.tissue.feature.wiki.application.dto.response.WikiTagResponse;
import com.tissue.feature.wiki.application.port.usecase.WikiTagCommandUseCase;
import com.tissue.feature.wiki.domain.exception.WikiErrorCode;
import com.tissue.global.openapi.WikiErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wiki Tag")
@RestController
@RequestMapping("/api/v1/wiki")
@RequiredArgsConstructor
public class WikiTagCommandController {

    private final WikiTagCommandUseCase wikiTagUseCase;

    @Operation(operationId = "attachWikiTag", summary = "Attach tag", description = """
                Attach a tag to a wiki document. If no tag with the same name exists yet, it is
                created. Any active member can attach tags.

                **Requirements:**
                - A document can have a maximum of 5 tags""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tag attached"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Tag limit exceeded", content = @Content)
    })
    @WikiErrors({
        WikiErrorCode.DOCUMENT_NOT_FOUND,
        WikiErrorCode.DOCUMENT_LOCKED,
        WikiErrorCode.DOCUMENT_TAG_LIMIT_EXCEEDED,
    })
    @PostMapping("/{wikiId}/tags")
    public ResponseEntity<WikiTagResponse> attachWikiTag(
            @PathVariable Long wikiId,
            @RequestBody @Valid AttachWikiTagRequest request,
            @CurrentMember MemberDetails memberDetails) {
        WikiTagResponse response = wikiTagUseCase.attachTag(wikiId, request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "detachWikiTag",
            summary = "Detach tag",
            description = "Detach a tag from a wiki document. The tag itself remains in the catalog.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tag detached"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WikiErrors({
        WikiErrorCode.DOCUMENT_NOT_FOUND,
        WikiErrorCode.DOCUMENT_LOCKED,
        WikiErrorCode.TAG_NOT_FOUND,
    })
    @DeleteMapping("/{wikiId}/tags/{tagId}")
    public ResponseEntity<Void> detachWikiTag(
            @PathVariable Long wikiId, @PathVariable Long tagId, @CurrentMember MemberDetails memberDetails) {
        wikiTagUseCase.detachTag(wikiId, tagId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
