package com.tissue.feature.comment.web;

import com.tissue.feature.comment.application.dto.response.MyCommentResponse;
import com.tissue.feature.comment.application.port.usecase.CommentQueryUseCase;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Comment")
@RestController
@RequestMapping("/api/v1/me/comments")
@RequiredArgsConstructor
public class MyCommentController {

    private final CommentQueryUseCase commentQueryUseCase;

    @Operation(operationId = "listMyComments", summary = "List my comments", description = """
                Retrieve all of the current user's comments with offset-based pagination.

                **Pagination parameters:**
                - `page` — Page number (0-indexed, default: 0)
                - `size` — Number of items per page (default: 20)
                - `sort` — Sort criteria (ex: `createdAt,desc`)""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Comments retrieved")})
    @GetMapping
    public ResponseEntity<Page<MyCommentResponse>> listMyComments(
            @CurrentMember MemberDetails memberDetails, @PageableDefault(size = 20) Pageable pageable) {
        Page<MyCommentResponse> response = commentQueryUseCase.getMyComments(memberDetails.getMemberId(), pageable);

        return ResponseEntity.ok(response);
    }
}
