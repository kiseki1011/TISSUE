package com.tissue.feature.tag.web;

import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.tag.application.dto.response.TagDetail;
import com.tissue.feature.tag.application.port.usecase.TagQueryUseCase;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tag")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class TagQueryController {

    private final TagQueryUseCase tagQueryUseCase;

    @Operation(operationId = "listTags", summary = "List tags", description = """
                    List tags of a project. Default sort is name asc.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tags retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @GetMapping("projects/{projectKey}/tags")
    public ResponseEntity<Page<TagDetail>> listTags(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            Pageable pageable,
            @CurrentMember MemberDetails memberDetails) {
        Page<TagDetail> tags = tagQueryUseCase.getTagsByProject(
                ProjectIdentifier.of(workspaceKey, projectKey), pageable, memberDetails.getMemberId());

        return ResponseEntity.ok(tags);
    }
}
