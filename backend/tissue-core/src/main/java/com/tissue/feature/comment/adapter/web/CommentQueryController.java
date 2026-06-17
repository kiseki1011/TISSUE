package com.tissue.feature.comment.adapter.web;

import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.comment.application.port.usecase.CommentQueryUseCase;
import com.tissue.feature.issue.domain.exception.IssueErrorCode;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.global.openapi.IssueErrors;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.PageResponse;
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
@RequestMapping("/api/v1/issues/{issueKey}")
@RequiredArgsConstructor
public class CommentQueryController {

    private final CommentQueryUseCase commentQueryUseCase;

    @Operation(operationId = "listIssueComments", summary = "List issue comments", description = """
                    List root comments on an issue. Each root comment includes its replies nested \
                    (depth is constrained to 1).

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comments retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @IssueErrors({IssueErrorCode.ISSUE_NOT_FOUND})
    @GetMapping("/comments")
    public ResponseEntity<PageResponse<CommentDetailResponse>> listIssueComments(
            @PathVariable String issueKey, Pageable pageable, @CurrentMember MemberDetails memberDetails) {
        Page<CommentDetailResponse> response = commentQueryUseCase.getIssueComments(
                IssueIdentifier.ofIssueKey(issueKey), pageable, memberDetails.getMemberId());

        return ResponseEntity.ok(PageResponse.from(response));
    }
}
