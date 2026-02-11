package com.tissue.comment.web;

import com.tissue.comment.web.request.AddCommentRequest;
import com.tissue.comment.web.request.UpdateCommentRequest;
import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.comment.application.port.usecase.CommentCommandUseCase;
import com.tissue.feature.comment.application.port.usecase.CommentQueryUseCase;
import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.project.web.resolver.CurrentProjectMember;
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
    public ResponseEntity<CommentCreateResponse> add(
            @PathVariable String issueKey,
            @RequestBody @Valid AddCommentRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand();
        CommentCreateResponse response = commentCommandUseCase.create(issueKey, command, currentProjectMember);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<Void> update(
            @PathVariable Long commentId,
            @PathVariable String issueKey,
            @RequestBody @Valid UpdateCommentRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        commentCommandUseCase.update(issueKey, commentId, request.content(), currentProjectMember);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long commentId,
            @PathVariable String issueKey,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        commentCommandUseCase.delete(issueKey, commentId, currentProjectMember);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CommentDetailResponse>> getComments(
            @PathVariable String issueKey, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        List<CommentDetailResponse> response = commentQueryUseCase.getIssueComments(issueKey, currentProjectMember);
        return ResponseEntity.ok(response);
    }
}
