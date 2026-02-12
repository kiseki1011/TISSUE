package com.tissue.comment.web;

import com.tissue.comment.web.request.AddCommentRequest;
import com.tissue.comment.web.request.UpdateCommentRequest;
import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.comment.application.port.usecase.CommentCommandUseCase;
import com.tissue.feature.comment.application.port.usecase.CommentQueryUseCase;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
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

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentCommandUseCase commentCommandUseCase;
    private final CommentQueryUseCase commentQueryUseCase;

    @PostMapping
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

    @PatchMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long commentId,
            @RequestBody @Valid UpdateCommentRequest request,
            @CurrentMember MemberDetails memberDetails) {
        commentCommandUseCase.update(
                IssueIdentifier.of(workspaceKey, issueKey), commentId, request.content(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @PathVariable Long commentId,
            @CurrentMember MemberDetails memberDetails) {
        commentCommandUseCase.delete(
                IssueIdentifier.of(workspaceKey, issueKey), commentId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CommentDetailResponse>> getIssueComments(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        List<CommentDetailResponse> response = commentQueryUseCase.getIssueComments(
                IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
