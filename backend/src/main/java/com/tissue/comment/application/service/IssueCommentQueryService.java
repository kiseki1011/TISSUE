package com.tissue.comment.application.service;

import com.tissue.comment.application.dto.out.CommentDetailResponse;
import com.tissue.comment.application.dto.out.MyCommentResponse;
import com.tissue.comment.application.port.in.CommentQueryUseCase;
import com.tissue.comment.application.port.out.CommentQueryRepository;
import com.tissue.comment.domain.Comment;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueCommentQueryService implements CommentQueryUseCase {

    private final CommentQueryRepository commentQueryRepository;

    @Override
    public List<CommentDetailResponse> getIssueComments(String workspaceKey, String projectKey, String issueKey) {
        List<Comment> allComments = commentQueryRepository.findByIssue(workspaceKey, issueKey);

        Map<Long, List<Comment>> repliesByParentId = allComments.stream()
                .filter(c -> c.getParentComment() != null)
                .collect(Collectors.groupingBy(c -> c.getParentComment().getId()));

        return allComments.stream()
                .filter(c -> c.getParentComment() == null) // Root comments
                .map(root -> {
                    List<Comment> replies = repliesByParentId.getOrDefault(root.getId(), List.of());
                    List<CommentDetailResponse> replyDtos = replies.stream()
                            .map(reply -> CommentDetailResponse.from(reply, List.of()))
                            .toList();
                    return CommentDetailResponse.from(root, replyDtos);
                })
                .toList();
    }

    @Override
    public Page<MyCommentResponse> getMyComments(Long memberId, Pageable pageable) {
        return commentQueryRepository.findByAuthor(memberId, pageable).map(MyCommentResponse::from);
    }
}
