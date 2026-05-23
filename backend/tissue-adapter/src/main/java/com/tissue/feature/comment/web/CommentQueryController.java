package com.tissue.feature.comment.web;

import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.comment.application.port.usecase.CommentQueryUseCase;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.global.openapi.WorkspaceErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
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

@Tag(name = "Comment")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}")
@RequiredArgsConstructor
public class CommentQueryController {

    private final CommentQueryUseCase commentQueryUseCase;

    @Operation(operationId = "listIssueComments", summary = "List issue comments", description = """
                    List root comments on an issue. Each root comment includes its replies nested \
                    (depth is constrained to 1).

                    **Requirements:**
                    - Requires workspace membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comments retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @GetMapping("/comments")
    public ResponseEntity<Page<CommentDetailResponse>> listIssueComments(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            Pageable pageable,
            @CurrentMember MemberDetails memberDetails) {
        Page<CommentDetailResponse> response = commentQueryUseCase.getIssueComments(
                IssueIdentifier.of(workspaceKey, issueKey), pageable, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
