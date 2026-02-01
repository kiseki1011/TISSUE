package com.tissue.comment.adapter.web;

import com.tissue.comment.adapter.web.dto.AddCommentRequest;
import com.tissue.comment.adapter.web.dto.UpdateCommentRequest;
import com.tissue.comment.application.dto.request.DeleteCommentCommand;
import com.tissue.comment.application.dto.request.UpdateCommentCommand;
import com.tissue.comment.application.dto.response.CommentAddResponse;
import com.tissue.comment.application.dto.response.CommentDetailResponse;
import com.tissue.comment.application.port.in.CommentCommandUseCase;
import com.tissue.comment.application.port.in.CommentQueryUseCase;
import com.tissue.project.adapter.web.resolver.CurrentProjectMember;
import com.tissue.project.application.dto.ProjectMemberContext;
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
            @PathVariable String issueKey,
            @RequestBody @Valid AddCommentRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand(issueKey, currentProjectMember);
        CommentAddResponse response = commentCommandUseCase.add(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<Void> update(
            @PathVariable Long commentId,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCommentRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new UpdateCommentCommand(issueKey, commentId, request.content(), currentProjectMember);
        commentCommandUseCase.update(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long commentId,
            @PathVariable String issueKey,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new DeleteCommentCommand(issueKey, commentId, currentProjectMember);
        commentCommandUseCase.delete(command);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CommentDetailResponse>> getComments(
            @PathVariable String issueKey, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        List<CommentDetailResponse> response = commentQueryUseCase.getIssueComments(issueKey, currentProjectMember);
        return ResponseEntity.ok(response);
    }
}
