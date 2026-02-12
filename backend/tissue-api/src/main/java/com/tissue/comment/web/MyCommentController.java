package com.tissue.comment.web;

import com.tissue.feature.comment.application.dto.response.MyCommentResponse;
import com.tissue.feature.comment.application.port.usecase.CommentQueryUseCase;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/comments")
@RequiredArgsConstructor
public class MyCommentController {

    private final CommentQueryUseCase commentQueryUseCase;

    @GetMapping
    public ResponseEntity<Page<MyCommentResponse>> getMyComments(
            @CurrentMember MemberDetails memberDetails,
            @RequestParam String workspaceKey,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MyCommentResponse> response =
                commentQueryUseCase.getMyComments(workspaceKey, memberDetails.getMemberId(), pageable);
        return ResponseEntity.ok(response);
    }
}
