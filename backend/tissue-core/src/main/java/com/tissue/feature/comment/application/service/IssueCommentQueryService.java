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
    public Page<CommentDetailResponse> getIssueComments(IssueIdentifier iid, Pageable pageable, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(iid.workspaceKey(), actorMemberId);

        Page<Comment> roots = commentQueryRepository.findRootsByIssue(iid.workspaceKey(), iid.issueKey(), pageable);
        if (roots.isEmpty()) {
            return roots.map(c -> CommentDetailResponse.from(c, List.of()));
        }

        List<Long> rootIds = roots.getContent().stream().map(Comment::getId).toList();
        Map<Long, List<Comment>> repliesByParentId = commentQueryRepository.findRepliesByParentIds(rootIds).stream()
                .collect(Collectors.groupingBy(c -> c.getParentComment().getId()));

        return roots.map(root -> {
            List<CommentDetailResponse> replyDtos = repliesByParentId.getOrDefault(root.getId(), List.of()).stream()
                    .map(reply -> CommentDetailResponse.from(reply, List.of()))
                    .toList();
            return CommentDetailResponse.from(root, replyDtos);
        });
    }

    @Override
    public Page<MyCommentResponse> getMyComments(String workspaceKey, Long actorMemberId, Pageable pageable) {
        return commentQueryRepository
                .findAllByWorkspaceKeyAndMemberId(workspaceKey, actorMemberId, pageable)
                .map(MyCommentResponse::from);
    }
}
