package com.tissue.feature.wiki.web;

import com.tissue.feature.wiki.application.dto.response.WikiBookmarkResponse;
import com.tissue.feature.wiki.application.port.usecase.WikiBookmarkQueryUseCase;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.global.openapi.WorkspaceErrors;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wiki Document")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/wiki")
@RequiredArgsConstructor
public class WikiBookmarkQueryController {

    private final WikiBookmarkQueryUseCase wikiBookmarkQueryUseCase;

    @Operation(operationId = "listWikiBookmarks", summary = "List bookmarked documents", description = """
                    List all wiki documents bookmarked by the current member.

                    **Requirements:**
                    - Requires workspace membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bookmarked documents retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @GetMapping("/bookmarks")
    public ResponseEntity<List<WikiBookmarkResponse>> listWikiBookmarks(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        List<WikiBookmarkResponse> response =
                wikiBookmarkQueryUseCase.getBookmarks(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
