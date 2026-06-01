package com.tissue.feature.comment.adapter.web;

import com.tissue.feature.comment.adapter.web.request.AddCommentRequest;
import com.tissue.feature.comment.adapter.web.request.UpdateCommentRequest;
import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.feature.comment.application.port.usecase.CommentCommandUseCase;
import com.tissue.feature.comment.domain.exception.CommentErrorCode;
import com.tissue.feature.issue.domain.exception.IssueErrorCode;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.global.openapi.CommentErrors;
import com.tissue.global.openapi.IssueErrors;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Comment")
@RestController
@RequestMapping("/api/v1/issues/{issueKey}")
@RequiredArgsConstructor
public class CommentCommandController {

    private final CommentCommandUseCase commentCommandUseCase;

    @Operation(operationId = "createComment", summary = "Add comment", description = """
                Add a new comment to an issue.

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Comment created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @IssueErrors({IssueErrorCode.ISSUE_NOT_FOUND})
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND, ProjectErrorCode.PROJECT_ARCHIVED})
    @CommentErrors({
        CommentErrorCode.COMMENT_NOT_FOUND,
        CommentErrorCode.NESTED_COMMENT_LIMIT_EXCEEDED,
        CommentErrorCode.COMMENT_PARENT_ISSUE_MISMATCH,
    })
    @PostMapping("/comments")
    public ResponseEntity<CommentCreateResponse> createComment(
            @PathVariable String issueKey,
            @RequestBody @Valid AddCommentRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        CommentCreateResponse response = commentCommandUseCase.create(
                IssueIdentifier.ofIssueKey(issueKey), command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateComment", summary = "Update comment", description = """
                    Update the content of an existing comment.

                    **Requirements:**
                    - Requires being the comment author, a project `MANAGER`, or system `ADMIN`""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Comment updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND, ProjectErrorCode.PROJECT_ARCHIVED})
    @CommentErrors({
        CommentErrorCode.COMMENT_NOT_FOUND,
        CommentErrorCode.COMMENT_EDIT_NOT_ALLOWED,
    })
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable String issueKey,
            @PathVariable Long commentId,
            @RequestBody @Valid UpdateCommentRequest request,
            @CurrentMember MemberDetails memberDetails) {
        commentCommandUseCase.update(
                IssueIdentifier.ofIssueKey(issueKey), commentId, request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteComment", summary = "Delete comment", description = """
                    Soft-delete a comment.

                    **Requirements:**
                    - Requires being the comment author, a project `MANAGER`, or system `ADMIN`""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Comment deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @CommentErrors({
        CommentErrorCode.COMMENT_NOT_FOUND,
        CommentErrorCode.COMMENT_EDIT_NOT_ALLOWED,
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String issueKey, @PathVariable Long commentId, @CurrentMember MemberDetails memberDetails) {
        commentCommandUseCase.delete(IssueIdentifier.ofIssueKey(issueKey), commentId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
