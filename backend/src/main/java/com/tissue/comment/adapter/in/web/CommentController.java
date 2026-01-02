package com.tissue.comment.adapter.in.web;

import com.tissue.comment.adapter.in.web.dto.AddCommentRequest;
import com.tissue.comment.adapter.in.web.dto.UpdateCommentRequest;
import com.tissue.comment.application.dto.in.DeleteCommentCommand;
import com.tissue.comment.application.dto.in.UpdateCommentCommand;
import com.tissue.comment.application.dto.out.CommentAddResponse;
import com.tissue.comment.application.dto.out.CommentDetailResponse;
import com.tissue.comment.application.port.in.CommentCommandUseCase;
import com.tissue.comment.application.port.in.CommentQueryUseCase;
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
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/issues/{issueKey}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentCommandUseCase commentCommandUseCase;
    private final CommentQueryUseCase commentQueryUseCase;

    @PostMapping
    public ResponseEntity<CommentAddResponse> add(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestBody @Valid AddCommentRequest request) {
        var command = request.toCommand(workspaceKey, projectKey, issueKey);

        CommentAddResponse response = commentCommandUseCase.add(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<Void> update(@PathVariable Long commentId, @RequestBody @Valid UpdateCommentRequest request) {
        var command = new UpdateCommentCommand(commentId, request.content());
        commentCommandUseCase.update(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long commentId, @PathVariable String workspaceKey, @PathVariable String projectKey) {
        var command = new DeleteCommentCommand(commentId);
        commentCommandUseCase.delete(command);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CommentDetailResponse>> getComments(
            @PathVariable String workspaceKey, @PathVariable String projectKey, @PathVariable String issueKey) {
        List<CommentDetailResponse> response = commentQueryUseCase.getIssueComments(workspaceKey, projectKey, issueKey);
        return ResponseEntity.ok(response);
    }
}
