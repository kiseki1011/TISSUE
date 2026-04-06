package com.tissue.feature.comment.web;

import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.comment.application.port.usecase.CommentCommandUseCase;
import com.tissue.feature.comment.application.port.usecase.CommentQueryUseCase;
import com.tissue.feature.comment.web.request.AddCommentRequest;
import com.tissue.feature.comment.web.request.UpdateCommentRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
public class CommentController {

    private final CommentCommandUseCase commentCommandUseCase;
    private final CommentQueryUseCase commentQueryUseCase;

    @Operation(summary = "Add comment", description = "Add a new comment to an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Comment created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @PostMapping("/comments")
    public ResponseEntity<CommentCreateResponse> addComment(
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
            summary = "Update comment",
            description = "Update the content of an existing comment. Only the comment author can update.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Comment updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Not the comment author", content = @Content),
        @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content)
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

    @Operation(summary = "Delete comment", description = "Delete a comment. Only the comment author can delete.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Comment deleted"),
        @ApiResponse(responseCode = "403", description = "Not the comment author", content = @Content),
        @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content)
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

    @Operation(summary = "List issue comments", description = "Retrieve all comments on an issue.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comments retrieved"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @GetMapping("/comments")
    public ResponseEntity<List<CommentDetailResponse>> getIssueComments(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        List<CommentDetailResponse> response = commentQueryUseCase.getIssueComments(
                IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
