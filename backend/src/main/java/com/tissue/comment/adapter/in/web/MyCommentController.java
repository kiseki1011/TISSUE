package com.tissue.comment.adapter.in.web;

import com.tissue.comment.application.dto.out.MyCommentResponse;
import com.tissue.comment.application.port.in.CommentQueryUseCase;
import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.resolver.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/comments")
@RequiredArgsConstructor
public class MyCommentController {

    private final CommentQueryUseCase commentQueryUseCase;

    @GetMapping
    public ResponseEntity<Page<MyCommentResponse>> getMyComments(
            @CurrentMember MemberUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MyCommentResponse> response =
                commentQueryUseCase.getMyComments(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(response);
    }
}
