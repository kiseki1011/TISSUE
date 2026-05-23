package com.tissue.feature.comment.web;

import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.feature.comment.application.port.usecase.CommentCommandUseCase;
import com.tissue.feature.comment.domain.exception.CommentErrorCode;
import com.tissue.feature.comment.web.request.AddCommentRequest;
import com.tissue.feature.comment.web.request.UpdateCommentRequest;
import com.tissue.feature.issue.domain.exception.IssueErrorCode;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.global.openapi.CommentErrors;
import com.tissue.global.openapi.IssueErrors;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.global.openapi.WorkspaceErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
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
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}")
@RequiredArgsConstructor
public class CommentCommandController {

    private final CommentCommandUseCase commentCommandUseCase;

    @Operation(operationId = "createComment", summary = "Add comment", description = "Add a new comment to an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Comment created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @IssueErrors({IssueErrorCode.ISSUE_NOT_FOUND})
    @ProjectErrors({ProjectErrorCode.PROJECT_ARCHIVED})
    @CommentErrors({
        CommentErrorCode.COMMENT_NOT_FOUND,
        CommentErrorCode.NESTED_COMMENT_LIMIT_EXCEEDED,
        CommentErrorCode.COMMENT_PARENT_ISSUE_MISMATCH,
    })
    @PostMapping("/comments")
    public ResponseEntity<CommentCreateResponse> createComment(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @RequestBody @Valid AddCommentRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        CommentCreateResponse response = commentCommandUseCase.create(
                IssueIdentifier.of(workspaceKey, issueKey), command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            operationId = "updateComment",
            summary = "Update comment",
            description = "Update the content of an existing comment. Only the comment author can update.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Comment updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @ProjectErrors({ProjectErrorCode.PROJECT_ARCHIVED})
    @CommentErrors({
        CommentErrorCode.COMMENT_NOT_FOUND,
        CommentErrorCode.COMMENT_EDIT_NOT_ALLOWED,
    })
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long commentId,
            @RequestBody @Valid UpdateCommentRequest request,
            @CurrentMember MemberDetails memberDetails) {
        commentCommandUseCase.update(
                IssueIdentifier.of(workspaceKey, issueKey),
                commentId,
                request.toCommand(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "deleteComment",
            summary = "Delete comment",
            description = "Soft-delete a comment. Only the comment author can delete.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Comment deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @CommentErrors({
        CommentErrorCode.COMMENT_NOT_FOUND,
        CommentErrorCode.COMMENT_EDIT_NOT_ALLOWED,
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long commentId,
            @CurrentMember MemberDetails memberDetails) {
        commentCommandUseCase.delete(
                IssueIdentifier.of(workspaceKey, issueKey), commentId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
