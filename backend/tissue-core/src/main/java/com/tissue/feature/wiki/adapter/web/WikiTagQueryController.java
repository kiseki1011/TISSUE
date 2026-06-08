package com.tissue.feature.wiki.adapter.web;

import com.tissue.feature.wiki.application.dto.response.WikiTagDetail;
import com.tissue.feature.wiki.application.port.usecase.WikiTagQueryUseCase;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wiki Tag")
@RestController
@RequestMapping("/api/v1/wiki")
@RequiredArgsConstructor
public class WikiTagQueryController {

    private final WikiTagQueryUseCase wikiTagQueryUseCase;

    @Operation(operationId = "searchWikiTags", summary = "Search wiki tags", description = """
                Search global wiki tags by name for autocomplete.
                Omit `keyword` to list all tags. Default sort is name asc.""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Tags retrieved")})
    @GetMapping("/tags")
    public ResponseEntity<Page<WikiTagDetail>> searchWikiTags(
            @RequestParam(required = false) @Nullable String keyword,
            Pageable pageable,
            @CurrentMember MemberDetails memberDetails) {
        Page<WikiTagDetail> tags = wikiTagQueryUseCase.searchTags(keyword, pageable, memberDetails.getMemberId());

        return ResponseEntity.ok(tags);
    }
}
