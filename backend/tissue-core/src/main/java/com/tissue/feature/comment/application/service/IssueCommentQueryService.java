package com.tissue.feature.comment.application.service;

import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.comment.application.dto.response.MyCommentResponse;
import com.tissue.feature.comment.application.port.repository.CommentQueryRepository;
import com.tissue.feature.comment.application.port.usecase.CommentQueryUseCase;
import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueCommentQueryService implements CommentQueryUseCase {

    private final CommentQueryRepository commentQueryRepository;
    private final WorkspaceMemberFinder workspaceMemberFinder;

    @Override
    public List<CommentDetailResponse> getIssueComments(IssueIdentifier issueIdentifier, Long memberId) {
        workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);

        List<Comment> allComments =
                commentQueryRepository.findByIssue(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        Map<Long, List<Comment>> repliesByParentId = allComments.stream()
                .filter(c -> c.getParentComment() != null)
                .collect(Collectors.groupingBy(c -> c.getParentComment().getId()));

        return allComments.stream()
                .filter(c -> c.getParentComment() == null)
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
    public Page<MyCommentResponse> getMyComments(String workspaceKey, Long memberId, Pageable pageable) {
        return commentQueryRepository
                .findAllByWorkspaceKeyAndMemberId(workspaceKey, memberId, pageable)
                .map(MyCommentResponse::from);
    }
}
