package com.tissue.feature.comment.application.service;

import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.comment.application.dto.response.MyCommentResponse;
import com.tissue.feature.comment.application.port.repository.CommentQueryRepository;
import com.tissue.feature.comment.application.port.usecase.CommentQueryUseCase;
import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
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
    private final IssueFinder issueFinder;
    private final ProjectMemberFinder projectMemberFinder;

    @Override
    public Page<CommentDetailResponse> getIssueComments(IssueIdentifier iid, Pageable pageable, Long actorMemberId) {
        Issue issue = issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey());
        projectMemberFinder.getBy(issue.getProject(), actorMemberId);

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
